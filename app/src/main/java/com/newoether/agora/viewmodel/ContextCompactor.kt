package com.newoether.agora.viewmodel

import com.newoether.agora.api.ProviderConfig
import com.newoether.agora.api.StreamEvent
import com.newoether.agora.api.LocalModelSerializer
import com.newoether.agora.data.CompactionConfig
import com.newoether.agora.data.CompactionMarker
import com.newoether.agora.data.SettingsManager
import com.newoether.agora.data.local.MessageEntity
import com.newoether.agora.data.repository.ConversationRepository
import com.newoether.agora.data.repository.SettingsRepository
import com.newoether.agora.model.ChatMessage
import com.newoether.agora.model.MessageStatus
import com.newoether.agora.model.ModelId
import com.newoether.agora.model.Participant
import com.newoether.agora.util.Constants
import com.newoether.agora.util.DebugLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

object ContextCompactorConstants {
    /** Prefix used for the synthetic summary message folded into a request path. */
    const val SUMMARY_MESSAGE_PREFIX = "compact_summary_"

    /** Character budget we refuse to exceed for an LLM summarizer's input transcript. */
    const val MAX_LLM_SUMMARY_INPUT_CHARS = 24_000

    /** Cap on the deterministic digest length so a huge fold cannot blow up the request. */
    const val MAX_DETERMINISTIC_SUMMARY_LENGTH = 12_000

    /** Per-message excerpt cap when building a deterministic digest. */
    const val PER_MESSAGE_EXCERPT_CHARS = 400

    /** Default token context used when no per-model size is known. */
    const val DEFAULT_AUTO_CONTEXT = 32_768

    /** Per-message excerpt cap of an LLM transcription version. */
    const val PER_MESSAGE_TRANSCRIPT_CHARS = 2_000
}

/**
 * Deterministic-per-budget token estimator used by compaction planning and the UI token counter.
 *
 * The estimate is intentionally coarse and stable: 1 token ≈ 4 characters of visible content plus a
 * small fixed overhead per message and a flat cost per attached image. It never changes between
 * runs, which is what lets the same user + same budget produce the same fold boundary.
 */
object CompactionTokenEstimator {
    const val CHARS_PER_TOKEN = 4.0
    const val PER_MESSAGE_OVERHEAD = 2
    const val PER_IMAGE_TOKENS = 85

    fun estimate(message: ChatMessage): Int {
        var tokens = (message.text.length / CHARS_PER_TOKEN).toInt()
        message.thoughts?.let { tokens += (it.length / CHARS_PER_TOKEN).toInt() }
        message.segments?.forEach { segment ->
            tokens += (segment.content.length / CHARS_PER_TOKEN).toInt()
            segment.toolArgs?.let { tokens += (it.length / CHARS_PER_TOKEN).toInt() }
            segment.toolResult?.let { tokens += (it.length / CHARS_PER_TOKEN).toInt() }
        }
        tokens += message.images.size * PER_IMAGE_TOKENS
        return tokens + PER_MESSAGE_OVERHEAD
    }

    fun estimate(messages: List<ChatMessage>): Int = messages.sumOf(::estimate)

    fun estimateText(text: String): Int = (text.length / CHARS_PER_TOKEN).toInt() + PER_MESSAGE_OVERHEAD
}

/** Approximate context windows for common model families, keyed by model-id fragment. */
internal object AutoContextBudget {
    const val DEFAULT = ContextCompactorConstants.DEFAULT_AUTO_CONTEXT

    val KNOWN = mapOf(
        "gpt-4o" to 128_000,
        "gpt-4o-mini" to 128_000,
        "gpt-4.1" to 1_028_000,
        "gpt-4" to 8_192,
        "gpt-3.5" to 16_385,
        "o1" to 200_000,
        "o3" to 200_000,
        "o4" to 200_000,
        "claude" to 200_000,
        "haiku" to 200_000,
        "sonnet" to 200_000,
        "opus" to 200_000,
        "gemini" to 1_000_000,
        "deepseek" to 64_000,
        "llama" to 32_768,
        "qwen" to 32_768,
        "mistral" to 32_768,
    )

    fun forModel(modelId: String?): Int {
        if (modelId.isNullOrBlank()) return DEFAULT
        val lower = modelId.lowercase()
        for ((fragment, size) in KNOWN) {
            if (lower.contains(fragment)) return size
        }
        return DEFAULT
    }
}

/**
 * Resolves the effective token context budget for a compaction run.
 *
 * - `manual` limit mode: the user-supplied token count.
 * - otherwise: an explicit configured token budget, else the per-model heuristic.
 */
internal fun resolveCompactionContextBudget(
    config: CompactionConfig,
    modelId: String? = null,
): Int = when {
    config.limitMode == SettingsManager.COMPACTION_LIMIT_MANUAL ->
        config.manualContextTokens.coerceAtLeast(512)
    config.tokenSize > 0 -> config.tokenSize
    else -> AutoContextBudget.forModel(modelId)
}

/**
 * Pure fold decision for one request path. Deterministic given the same inputs.
 *
 * Returns the number of OLDEST messages to fold into a summary (0 = no compaction needed). The
 * newest messages are never folded, and at least [CompactionConfig.keepRecent] messages are kept.
 */
internal fun computeFoldBoundary(
    config: CompactionConfig,
    messageCount: Int,
    estimatedTokens: Int,
    budgetContext: Int,
): Int {
    if (messageCount <= 1) return 0
    val keepMinimum = config.keepRecent.coerceIn(1, messageCount)
    val maxFoldable = messageCount - keepMinimum
    if (maxFoldable <= 0) return 0

    return when (config.strategy) {
        SettingsManager.COMPACTION_STRATEGY_MESSAGE_COUNT -> {
            val targetKeep = config.messageCount.coerceAtLeast(keepMinimum)
            (messageCount - targetKeep).coerceIn(0, maxFoldable)
        }
        else -> {
            val budget = config.effectiveTokenSize(budgetContext).coerceAtLeast(128)
            if (estimatedTokens <= budget) 0
            else {
                val averagePerMessage = (estimatedTokens.toDouble() / messageCount.toDouble()).coerceAtLeast(1.0)
                val excess = estimatedTokens - budget
                (excess / averagePerMessage).coerceIn(0.0, maxFoldable.toDouble()).toInt()
            }
        }
    }
}

/**
 * Owns context compaction: deciding the fold boundary, generating the summary (deterministic or via
 * an LLM), persisting per-conversation [CompactionMarker]s, and assembling request paths where the
 * pre-boundary messages collapse into a single summary user message.
 *
 * Markers live in DataStore (see [SettingsManager.compactionState]), never Room, so compaction never
 * requires a DB migration and the original messages stay untouched — removing the marker (revert)
 * instantly restores the full verbatim conversation.
 */
class ContextCompactor(
    private val settings: SettingsRepository,
    private val providers: ProviderRegistry,
    private val conversations: ConversationRepository,
) {

    /** Mirrors the persisted marker map so the UI can react to compaction changes. */
    val markers: StateFlow<Map<String, CompactionMarker>> get() = settings.compactionState

    /**
     * The main entry point used by the generation pipeline: turns a full request path into its
     * compacted twin (summary + verbatim tail) if compaction is warranted.
     *
     * [path] must be the ordered, provider-ready request messages (oldest first). [contextLimit] is
     * the effective token budget for auto mode.
     */
    suspend fun prepareRequest(
        conversationId: String,
        path: List<ChatMessage>,
        config: CompactionConfig,
        contextLimit: Int,
    ): RequestCompactionResult {
        if (!config.enabled || path.isEmpty()) return RequestCompactionResult.Reuse(path)

        val existing = settings.compactionState.value[conversationId]
        val boundaryIndex = existing?.boundaryMessageId?.let { boundary ->
            path.indexOfFirst { it.id == boundary }.takeIf { it > 0 }
        }
        if (existing != null && boundaryIndex != null) {
            // Same marker + verbatim tail: same user + same budget ⇒ same fold, no re-summary.
            return RequestCompactionResult.Compacted(
                path = foldedPath(path, existing, boundaryIndex, conversationId),
                marker = existing,
            )
        }

        val estimated = CompactionTokenEstimator.estimate(path)
        val foldCount = computeFoldBoundary(config, path.size, estimated, contextLimit)
        if (foldCount <= 0) return RequestCompactionResult.Reuse(path)

        val folded = path.take(foldCount)
        val summaryText = summarize(conversationId, folded, config).ifBlank { deterministicSummary(folded) }
        val marker = CompactionMarker(
            conversationId = conversationId,
            boundaryMessageId = path[foldCount].id,
            summaryText = summaryText,
            strategyUsed = config.strategy,
            summaryMode = config.summaryMode,
            contextLimit = contextLimit,
        )
        persistMarker(marker)
        return RequestCompactionResult.Compacted(foldedPath(path, marker, foldCount, conversationId), marker)
    }

    /**
     * Mid-generation recompaction: folds an in-flight path that has already grown past the budget.
     *
     * Unlike [prepareRequest], this deliberately ignores any existing marker shortcut — the path is
     * treated as a fresh candidate so that tool-round growth (accumulated result messages that never
     * went through a request-time fold) can be folded again. The oldest messages collapse into a new
     * summary, its boundary is persisted, and the caller continues generation from the compacted twin
     * (summary + verbatim tail) rather than aborting the run.
     */
    suspend fun foldInFlightPath(
        conversationId: String,
        path: List<ChatMessage>,
        config: CompactionConfig,
        contextLimit: Int,
    ): RequestCompactionResult {
        if (!config.enabled || path.isEmpty()) return RequestCompactionResult.Reuse(path)
        val estimated = CompactionTokenEstimator.estimate(path)
        val foldCount = computeFoldBoundary(config, path.size, estimated, contextLimit)
        if (foldCount <= 0) return RequestCompactionResult.Reuse(path)

        val folded = path.take(foldCount)
        val summaryText = summarize(conversationId, folded, config).ifBlank { deterministicSummary(folded) }
        val marker = CompactionMarker(
            conversationId = conversationId,
            boundaryMessageId = path[foldCount].id,
            summaryText = summaryText,
            strategyUsed = config.strategy,
            summaryMode = config.summaryMode,
            contextLimit = contextLimit,
        )
        persistMarker(marker)
        return RequestCompactionResult.Compacted(foldedPath(path, marker, foldCount, conversationId), marker)
    }

    /** Manual "compact now": context unconditionally, respecting only the keep-recent margin. */
    suspend fun compactNow(
        conversationId: String,
        config: CompactionConfig,
        contextLimit: Int,
    ): CompactionMarker? {
        if (!config.enabled) return settings.compactionState.value[conversationId]
        val snapshot = conversations.getMessagesForConversationSnapshot(conversationId)
        val path = entitiesToPath(snapshot)
        if (path.size <= 1) return settings.compactionState.value[conversationId]

        val existing = settings.compactionState.value[conversationId]
        val boundaryIndex = existing?.boundaryMessageId?.let { boundary ->
            path.indexOfFirst { it.id == boundary }.takeIf { it > 0 }
        }
        if (existing != null && boundaryIndex != null) return existing

        val keepMinimum = config.keepRecent.coerceIn(1, path.size)
        val foldCount = (path.size - keepMinimum).coerceAtLeast(1).coerceAtMost(path.size - 1)
        val folded = path.take(foldCount)
        if (folded.isEmpty()) return settings.compactionState.value[conversationId]

        val summaryText = summarize(conversationId, folded, config).ifBlank { deterministicSummary(folded) }
        val marker = CompactionMarker(
            conversationId = conversationId,
            boundaryMessageId = path[foldCount].id,
            summaryText = summaryText,
            strategyUsed = config.strategy,
            summaryMode = config.summaryMode,
            contextLimit = contextLimit,
        )
        persistMarker(marker)
        return marker
    }

    /** Removes the marker for a conversation, restoring the full verbatim context. */
    suspend fun revertCompaction(conversationId: String) {
        if (conversationId !in settings.compactionState.value) return
        val updated = settings.compactionState.value.toMutableMap()
        updated.remove(conversationId)
        settings.saveCompactionState(updated)
    }

    /** For the UI token counter: how many tokens the persisted summary represents. */
    fun summaryTokens(conversationId: String): Int {
        val marker = settings.compactionState.value[conversationId] ?: return 0
        return CompactionTokenEstimator.estimateText(marker.summaryText)
    }

    // ── Internals ─────────────────────────────────────────────

    private suspend fun summarize(
        conversationId: String,
        folded: List<ChatMessage>,
        config: CompactionConfig,
    ): String =
        when (config.summaryMode) {
            SettingsManager.COMPACTION_SUMMARY_LLM -> summarizeWithLlm(conversationId, folded, config)
            else -> deterministicSummary(folded)
        }

    /** The model the conversation itself is currently running on (the chatbox selector pick). */
    private suspend fun conversationActiveModel(conversationId: String): String? =
        conversations.getConversation(conversationId)?.modelId?.takeIf { it.isNotBlank() }

    /** Deterministic digest: one line per message, bounded and stable. */
    private fun deterministicSummary(messages: List<ChatMessage>): String {
        var budget = ContextCompactorConstants.MAX_DETERMINISTIC_SUMMARY_LENGTH
        val lines = mutableListOf<String>()
        for (message in messages) {
            if (budget <= 0) break
            val line = renderMessageForSummary(message)
            if (line.isBlank()) continue
            lines += line
            budget -= line.length
        }
        return lines.joinToString("\n").take(ContextCompactorConstants.MAX_DETERMINISTIC_SUMMARY_LENGTH)
    }

    private fun renderMessageForSummary(message: ChatMessage): String {
        val role = when (message.participant) {
            Participant.USER -> "User"
            Participant.MODEL -> "Assistant"
            Participant.ERROR -> "Error"
        }
        val excerpt = message.text.trim().take(ContextCompactorConstants.PER_MESSAGE_EXCERPT_CHARS)
        val images = if (message.images.isNotEmpty()) " [image×${message.images.size}]" else ""
        val tool = message.toolCall?.let { call ->
            " [tool: ${call.toolName} → ${brief(call.resultText ?: call.result)}]"
        } ?: ""
        return "$role: $excerpt$images$tool"
    }

    private fun brief(value: String): String {
        val cleaned = value.replace(Regex("\\s+"), " ").trim()
        return if (cleaned.length > 240) cleaned.take(240) + "…" else cleaned
    }

    private fun foldedPath(
        path: List<ChatMessage>,
        marker: CompactionMarker,
        boundary: Int,
        conversationId: String,
    ): List<ChatMessage> {
        val summaryMessage = ChatMessage(
            id = ContextCompactorConstants.SUMMARY_MESSAGE_PREFIX + conversationId,
            parentId = null,
            text = marker.summaryText,
            participant = Participant.USER,
            status = MessageStatus.SUCCESS,
            timestamp = 0L,
        )
        return buildList {
            add(summaryMessage)
            addAll(path.drop(boundary))
        }
    }

    private suspend fun persistMarker(marker: CompactionMarker) {
        val current = settings.compactionState.value.toMutableMap()
        current[marker.conversationId] = marker
        settings.saveCompactionState(current)
    }

    private suspend fun summarizeWithLlm(
        conversationId: String,
        folded: List<ChatMessage>,
        config: CompactionConfig,
    ): String {
        val configuredModel = config.llmModel?.takeIf { it.isNotBlank() }
            ?: conversationActiveModel(conversationId)
            ?: settings.selectedModel.value.takeIf { it.isNotBlank() }
            ?: return ""
        val providerName = providers.providerForModel(configuredModel)
        val activeKey = settings.awaitActiveKey(providerName)?.takeIf { it.isNotBlank() }
            ?: settings.resolveActiveKey(providerName).orEmpty()
        val provider = providers.getInstanceOrNull(providerName)
        if (provider == null || !providers.isConfigured(providerName, activeKey)) return ""

        val modelName = ModelId.parse(configuredModel).modelName
        val transcript = buildLlmTranscript(folded)
        val customInstructions = settings.compactionSummaryInstructions.value
        val prompt = listOf(
            ChatMessage(
                text = if (customInstructions.isBlank()) {
                    "You are compressing a chat transcript into a durable summary that a future " +
                        "turn of the SAME assistant must be able to continue from. Agora is a personal " +
                        "AI helper/agent: it chats, executes agentic tool calls (web search, code, " +
                        "remote shell/file I/O, image generation, memory), and runs recurring tasks on a " +
                        "schedule for the user, so the summary must preserve everything needed for the " +
                        "continuation of the whole role the user relies on — not just one chat.\n\n" +
                        "Write the summary in third person, neutral and technical but conversational in " +
                        "tone. Keep the same languages used in the transcript. Preserve exact facts, " +
                        "names, identifiers, keys of tasks, dates, deadlines, repeat intervals, numeric " +
                        "values, and tool output conclusions. Organize it into these sections, using the " +
                        "best-fitting ones and skipping any that have no content:\n" +
                        "1. 'Conversation Overview' — the main thread and how it progressed, including " +
                        "turns in focus.\n" +
                        "2. 'Active Goals & Current Task' — the immediate objective and where execution " +
                        "stands right now (steps done, steps remaining).\n" +
                        "3. 'Recurring Tasks & Scheduling' — any task expected to run again, its trigger, " +
                        "interval, and its current state.\n" +
                        "4. 'Key Facts & Context' — stable details about the user, their work, and the " +
                        "world that matter beyond this single conversation.\n" +
                        "5. 'Decisions & Preferences' — choices and preferences the user expressed, " +
                        "including tool/model usage, formatting, or tone.\n" +
                        "6. 'Tool & Environment Activity' — tool invocations, remote sessions, search " +
                        "findings, file/data locations, and setup details worth carrying forward.\n" +
                        "7. 'Open Questions & Pending Requests' — anything unresolved, waiting on the " +
                        "user, or requested but not yet delivered.\n" +
                        "8. 'Next Steps' — the concrete first actions the assistant should take when resuming.\n\n" +
                        "If a previous summary appears inside the transcript, treat it as source material " +
                        "and fold it in, removing superseded detail. Keep the summary comprehensive and " +
                        "specific. Output only the summary.\n\n$transcript"
                } else {
                    customInstructions.trim() + "\n\n$transcript"
                },
                participant = Participant.USER,
                status = MessageStatus.SUCCESS,
            )
        )
        val config = ProviderConfig(
            apiKey = activeKey,
            modelId = modelName,
            maxContextWindow = 1,
            thinkingEnabled = false,
            baseUrl = providers.getEffectiveBaseUrl(providerName),
        )

        var collected = StringBuilder()
        var providerError: String? = null
        suspend fun collect() {
            provider.generateResponse(prompt, config).collect { event ->
                when (event) {
                    is StreamEvent.TextChunk -> collected.append(event.text)
                    is StreamEvent.Error -> providerError = event.message
                    else -> Unit
                }
            }
        }
        return try {
            if (providerName == Constants.PROVIDER_LOCAL) {
                LocalModelSerializer.mutex.withLock {
                    withContext(Dispatchers.IO) { collect() }
                }
            } else {
                collect()
            }
            val text = collected.toString().trim()
            if (providerError != null || text.isBlank()) "" else text
        } catch (ignoredException: CancellationException) {
            throw ignoredException
        } catch (error: Exception) {
            DebugLog.e("ContextCompactor", "LLM summary failed for provider=$providerName", error)
            ""
        }
    }

    private fun buildLlmTranscript(folded: List<ChatMessage>): String {
        var budget = ContextCompactorConstants.MAX_LLM_SUMMARY_INPUT_CHARS
        val parts = mutableListOf<String>()
        val cap = ContextCompactorConstants.PER_MESSAGE_TRANSCRIPT_CHARS
        for (message in folded) {
            if (budget <= 0) break
            val role = when (message.participant) {
                Participant.USER -> "User"
                Participant.MODEL -> "Assistant"
                Participant.ERROR -> "Error"
            }
            var text = "$role: ${message.text}"
            if (!message.thoughts.isNullOrBlank()) text += "\nThought: ${message.thoughts}"
            message.toolCall?.let { call ->
                text += "\n[tool ${call.toolName}: ${brief(call.arguments)} → ${brief(call.result)}]"
            }
            val clipped = text.take(cap)
            parts += clipped
            budget -= clipped.length
        }
        return parts.joinToString("\n").take(ContextCompactorConstants.MAX_LLM_SUMMARY_INPUT_CHARS)
    }

    private fun entitiesToPath(entities: List<MessageEntity>): List<ChatMessage> =
        ConversationUiState.resolvePath(
            allMessages = entities.map { entity ->
                ChatMessage(
                    id = entity.id,
                    parentId = entity.parentId,
                    text = entity.text,
                    images = entity.images,
                    thoughts = entity.thoughts,
                    participant = entity.participant,
                    timestamp = entity.timestamp,
                    modelName = entity.modelName,
                    runId = entity.runId,
                    runSequence = entity.runSequence,
                )
            },
            streamingMsg = null,
            selectedChildren = emptyMap(),
        )
}

sealed interface RequestCompactionResult {
    data class Reuse(val path: List<ChatMessage>) : RequestCompactionResult
    data class Compacted(
        val path: List<ChatMessage>,
        val marker: CompactionMarker,
    ) : RequestCompactionResult
}