package com.newoether.agora.ui.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.newoether.agora.R
import com.newoether.agora.data.ConversationSettings
import com.newoether.agora.model.apiModelName
import com.newoether.agora.ui.settings.PillTabSwitcher
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun AdvancedSettingsDialog(
    overrides: ConversationSettings,
    globalDefaults: ConversationSettings,
    enabledModels: Set<String> = emptySet(),
    modelAliases: Map<String, String> = emptyMap(),
    onSave: (ConversationSettings) -> Unit,
    onResetToDefaults: () -> Unit,
    onDismiss: () -> Unit
) {
    var contextWindow by remember { mutableStateOf(overrides.contextWindow) }
    var temperature by remember { mutableStateOf(overrides.temperature) }
    var maxTokens by remember { mutableStateOf(overrides.maxTokens) }
    var topP by remember { mutableStateOf(overrides.topP) }
    var frequencyPenalty by remember { mutableStateOf(overrides.frequencyPenalty) }
    var presencePenalty by remember { mutableStateOf(overrides.presencePenalty) }
    var compactEnabled by remember { mutableStateOf(overrides.compactEnabled) }
    var compactStrategy by remember { mutableStateOf(overrides.compactStrategy) }
    var compactMessageCount by remember { mutableStateOf(overrides.compactMessageCount) }
    var compactTokenPercent by remember { mutableStateOf(overrides.compactTokenPercent) }
    var compactTokenSize by remember { mutableStateOf(overrides.compactTokenSize) }
    var compactSummaryMode by remember { mutableStateOf(overrides.compactSummaryMode) }
    var compactLlmModel by remember { mutableStateOf(overrides.compactLlmModel) }
    var compactKeepRecent by remember { mutableStateOf(overrides.compactKeepRecent) }
    var compactLimitMode by remember { mutableStateOf(overrides.compactLimitMode) }
    var compactManualTokens by remember { mutableStateOf(overrides.manualContextTokens) }

    fun currentSettings() = overrides.copy(
        contextWindow = contextWindow,
        temperature = temperature,
        maxTokens = maxTokens,
        topP = topP,
        frequencyPenalty = frequencyPenalty,
        presencePenalty = presencePenalty,
        compactEnabled = compactEnabled,
        compactStrategy = compactStrategy,
        compactMessageCount = compactMessageCount,
        compactTokenPercent = compactTokenPercent,
        compactTokenSize = compactTokenSize,
        compactSummaryMode = compactSummaryMode,
        compactLlmModel = compactLlmModel,
        compactKeepRecent = compactKeepRecent,
        compactLimitMode = compactLimitMode,
        manualContextTokens = compactManualTokens
    )

    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.advanced_generation_title), fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                val fmt2: (Float) -> String = { v -> String.format(Locale.US, "%.2f", v) }
                val gDefaults = globalDefaults

                // Context Window
                AdvancedParamRow(
                    label = stringResource(R.string.context_window),
                    value = contextWindow?.toFloat(),
                    defaultVal = gDefaults.contextWindow?.toFloat(),
                    valueRange = 5f..100f,
                    format = { it.toInt().toString() },
                    onChange = { contextWindow = it.toInt() },
                    onReset = { contextWindow = null }
                )
                // Temperature
                AdvancedParamRow(
                    label = stringResource(R.string.gen_temperature),
                    value = temperature,
                    defaultVal = gDefaults.temperature,
                    valueRange = 0f..2f,
                    format = fmt2,
                    onChange = { temperature = it },
                    onReset = { temperature = null }
                )
                // Max Tokens
                val maxTokensPresets = intArrayOf(256, 512, 1024, 2048, 4096, 8192, 16384, 32768)
                AdvancedParamRow(
                    label = stringResource(R.string.gen_max_tokens),
                    value = maxTokens,
                    defaultVal = gDefaults.maxTokens,
                    presets = maxTokensPresets,
                    format = { it.toString() },
                    onChange = { maxTokens = it },
                    onReset = { maxTokens = null }
                )
                // Top P
                AdvancedParamRow(
                    label = stringResource(R.string.gen_top_p),
                    value = topP,
                    defaultVal = gDefaults.topP,
                    valueRange = 0f..1f,
                    format = fmt2,
                    onChange = { topP = it },
                    onReset = { topP = null }
                )
                // Frequency Penalty
                AdvancedParamRow(
                    label = stringResource(R.string.gen_frequency_penalty),
                    value = frequencyPenalty,
                    defaultVal = gDefaults.frequencyPenalty,
                    valueRange = -2f..2f,
                    format = fmt2,
                    onChange = { frequencyPenalty = it },
                    onReset = { frequencyPenalty = null }
                )
                // Presence Penalty
                AdvancedParamRow(
                    label = stringResource(R.string.gen_presence_penalty),
                    value = presencePenalty,
                    defaultVal = gDefaults.presencePenalty,
                    valueRange = -2f..2f,
                    format = fmt2,
                    onChange = { presencePenalty = it },
                    onReset = { presencePenalty = null }
                )
                // Context Compaction
                CompactionControlPanel(
                    compactEnabled = compactEnabled,
                    strategy = compactStrategy,
                    messageCount = compactMessageCount,
                    tokenPercent = compactTokenPercent,
                    tokenSize = compactTokenSize,
                    summaryMode = compactSummaryMode,
                    llmModel = compactLlmModel,
                    keepRecent = compactKeepRecent,
                    limitMode = compactLimitMode,
                    manualTokens = compactManualTokens,
                    globalDefaults = gDefaults,
                    enabledModels = enabledModels,
                    modelAliases = modelAliases,
                    onEnabled = { compactEnabled = it },
                    onStrategy = { compactStrategy = it },
                    onMessageCount = { compactMessageCount = it },
                    onTokenPercent = { compactTokenPercent = it },
                    onTokenSize = { compactTokenSize = it },
                    onSummaryMode = { compactSummaryMode = it },
                    onSummaryModeReset = { compactSummaryMode = null },
                    onSummaryLlmModel = { compactLlmModel = it },
                    onKeepRecent = { compactKeepRecent = it },
                    onLimitMode = { compactLimitMode = it },
                    onManualTokens = { compactManualTokens = it }
                )
            }
        },
        confirmButton = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = {
                    onResetToDefaults()
                    contextWindow = null; temperature = null; maxTokens = null
                    topP = null; frequencyPenalty = null; presencePenalty = null
                    compactEnabled = null; compactStrategy = null; compactMessageCount = null
                    compactTokenPercent = null; compactTokenSize = null; compactSummaryMode = null
                    compactLlmModel = null; compactKeepRecent = null; compactLimitMode = null
                    compactManualTokens = null
                }) { Text(stringResource(R.string.gen_reset)) }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.provider_cancel))
                    }
                    TextButton(onClick = { onSave(currentSettings()) }) {
                        Text(stringResource(R.string.provider_save))
                    }
                }
            }
        },
        dismissButton = null
    )
}

/**
 * Parameter row for the Advanced dialog. Always shows value.
 * When value is null, shows the default value passed from global settings.
 * Reset clears the local override (doesn't close dialog).
 */
@Composable
private fun AdvancedParamRow(
    label: String,
    value: Float?,
    defaultVal: Float?,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    format: (Float) -> String,
    onChange: (Float) -> Unit,
    onReset: () -> Unit
) {
    val hasDefault = defaultVal != null
    val sliderPos = value ?: defaultVal ?: (valueRange.start + valueRange.endInclusive) / 2f
    val isOverride = value != null
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            if (isOverride) {
                Text(
                    text = format(sliderPos),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = stringResource(R.string.gen_reset),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    modifier = Modifier.clickable(onClick = onReset)
                )
            } else if (hasDefault) {
                Text(
                    text = format(sliderPos),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Normal
                )
            } else {
                Text(
                    text = stringResource(R.string.gen_not_specified),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
        Slider(
            value = sliderPos,
            onValueChange = onChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun AdvancedParamRow(
    label: String,
    value: Int?,
    defaultVal: Int?,
    presets: IntArray,
    format: (Int) -> String,
    onChange: (Int) -> Unit,
    onReset: () -> Unit
) {
    fun toIndex(v: Int) = presets.indices.minByOrNull { kotlin.math.abs(presets[it] - v) } ?: 3
    val hasDefault = defaultVal != null
    val effective = value ?: defaultVal ?: 4096
    val index = toIndex(effective)
    val isOverride = value != null
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            if (isOverride) {
                Text(
                    text = format(value!!),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = stringResource(R.string.gen_reset),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    modifier = Modifier.clickable(onClick = onReset)
                )
            } else if (hasDefault) {
                Text(
                    text = format(effective),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Normal
                )
            } else {
                Text(
                    text = stringResource(R.string.gen_not_specified),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
        Slider(
            value = index.toFloat(),
            onValueChange = { onChange(presets[it.toInt().coerceIn(0, presets.lastIndex)]) },
            valueRange = 0f..(presets.size - 1).toFloat(),
            steps = presets.size - 2,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Per-conversation compaction overrides shown inside the Advanced dialog. Every control is
 * tri-state: `null` inherits the resolved global default, otherwise it's an explicit override.
 */
@Composable
private fun CompactionControlPanel(
    compactEnabled: Boolean?,
    strategy: String?,
    messageCount: Int?,
    tokenPercent: Int?,
    tokenSize: Int?,
    summaryMode: String?,
    llmModel: String?,
    keepRecent: Int?,
    limitMode: String?,
    manualTokens: Int?,
    globalDefaults: ConversationSettings,
    enabledModels: Set<String> = emptySet(),
    modelAliases: Map<String, String> = emptyMap(),
    onEnabled: (Boolean?) -> Unit,
    onStrategy: (String?) -> Unit,
    onMessageCount: (Int) -> Unit,
    onTokenPercent: (Int) -> Unit,
    onTokenSize: (Int) -> Unit,
    onSummaryMode: (String?) -> Unit,
    onSummaryModeReset: () -> Unit,
    onSummaryLlmModel: (String?) -> Unit,
    onKeepRecent: (Int) -> Unit,
    onLimitMode: (String?) -> Unit,
    onManualTokens: (Int) -> Unit
) {
    val d = com.newoether.agora.data.SettingsManager
    var showCompactionModelDialog by remember { mutableStateOf(false) }

    // Effective values: an explicit per-conversation override wins, otherwise the resolved
    // global default is shown (never a hard-coded fallback).
    val effectiveStrategy = strategy ?: globalDefaults.compactStrategy ?: d.COMPACTION_STRATEGY_TOKEN_PERCENT
    val effectiveMessageCount = (messageCount ?: globalDefaults.compactMessageCount ?: 40).coerceIn(4, 64)
    val effectiveTokenPercent = (tokenPercent ?: globalDefaults.compactTokenPercent ?: 80).coerceIn(30, 90)
    val effectiveTokenSize = (tokenSize ?: globalDefaults.compactTokenSize ?: 8000).coerceIn(1000, 32000)
    val effectiveSummaryMode = summaryMode ?: globalDefaults.compactSummaryMode ?: d.COMPACTION_SUMMARY_DETERMINISTIC
    val effectiveKeepRecent = (keepRecent ?: globalDefaults.compactKeepRecent ?: 4).coerceIn(1, 20)
    val effectiveLimitMode = limitMode ?: globalDefaults.compactLimitMode ?: d.COMPACTION_LIMIT_AUTO
    val effectiveManualTokens = (manualTokens ?: globalDefaults.manualContextTokens ?: 6000).coerceIn(1000, 16000)

    // Enabled toggle: checked = "not explicitly disabled" so an untouched conversation
    // inherits the global auto-compaction setting while the user can still pin on/off.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.compaction_enabled),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = stringResource(R.string.compaction_enabled_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = compactEnabled != false,
            onCheckedChange = { onEnabled(if (it) true else false) }
        )
    }

    if (compactEnabled == false) return

    Column(modifier = Modifier.fillMaxWidth()) {
        // Strategy
        Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Text(
                text = stringResource(R.string.compaction_strategy_label),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(6.dp))
            PillTabSwitcher(
                tabs = listOf(
                    stringResource(R.string.compaction_strategy_messages_short),
                    stringResource(R.string.compaction_strategy_percent_short),
                    stringResource(R.string.compaction_strategy_tokens_short)
                ),
                selectedIndex = when (effectiveStrategy) {
                    d.COMPACTION_STRATEGY_MESSAGE_COUNT -> 0
                    d.COMPACTION_STRATEGY_TOKEN_PERCENT -> 1
                    else -> 2
                },
                onSelect = { index ->
                    onStrategy(when (index) {
                        0 -> d.COMPACTION_STRATEGY_MESSAGE_COUNT
                        1 -> d.COMPACTION_STRATEGY_TOKEN_PERCENT
                        else -> d.COMPACTION_STRATEGY_TOKEN_SIZE
                    })
                }
            )
        }
        Spacer(Modifier.height(12.dp))

        when (effectiveStrategy) {
            d.COMPACTION_STRATEGY_MESSAGE_COUNT -> {
                CompactionIntSlider(
                    label = stringResource(R.string.compaction_strategy_messages_value),
                    value = effectiveMessageCount,
                    range = 4..64,
                    onChange = onMessageCount
                )
            }
            d.COMPACTION_STRATEGY_TOKEN_PERCENT -> {
                CompactionIntSlider(
                    label = stringResource(R.string.compaction_strategy_percent_value),
                    value = effectiveTokenPercent,
                    range = 30..90,
                    percent = true,
                    onChange = onTokenPercent
                )
            }
            else -> {
                CompactionIntSlider(
                    label = stringResource(R.string.compaction_strategy_tokens_value),
                    value = effectiveTokenSize,
                    range = 1000..32000,
                    step = 1000,
                    onChange = onTokenSize
                )
            }
        }

        // Summary mode picker + model
        Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Text(
                text = stringResource(R.string.compaction_summary_mode),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(8.dp))
            PillTabSwitcher(
                tabs = listOf(
                    stringResource(R.string.compaction_summary_llm),
                    stringResource(R.string.compaction_summary_deterministic)
                ),
                selectedIndex = if (effectiveSummaryMode == d.COMPACTION_SUMMARY_LLM) 0 else 1,
                onSelect = { index ->
                    onSummaryMode(if (index == 0) d.COMPACTION_SUMMARY_LLM else d.COMPACTION_SUMMARY_DETERMINISTIC)
                }
            )
            if (effectiveSummaryMode == d.COMPACTION_SUMMARY_LLM) {
                val effectiveModelDisplayName = if (llmModel == null) {
                    stringResource(R.string.compaction_model_current)
                } else {
                    val alias = modelAliases[llmModel]
                    alias ?: com.newoether.agora.model.ModelId.parse(llmModel).apiModelName
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showCompactionModelDialog = true }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.compaction_model),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = effectiveModelDisplayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        if (showCompactionModelDialog) {
            val models = enabledModels.sorted()
            AlertDialog(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                onDismissRequest = { showCompactionModelDialog = false },
                title = { Text(stringResource(R.string.compaction_model), fontWeight = FontWeight.Bold) },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSummaryLlmModel(null)
                                    showCompactionModelDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            androidx.compose.material3.RadioButton(
                                selected = llmModel == null,
                                onClick = {
                                    onSummaryLlmModel(null)
                                    showCompactionModelDialog = false
                                },
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.compaction_model_current),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        models.forEach { model ->
                            val alias = modelAliases[model]
                            val parsed = com.newoether.agora.model.ModelId.parse(model)
                            val displayName = alias ?: parsed.apiModelName
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSummaryLlmModel(model)
                                        showCompactionModelDialog = false
                                    }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                androidx.compose.material3.RadioButton(
                                    selected = llmModel == model,
                                    onClick = {
                                        onSummaryLlmModel(model)
                                        showCompactionModelDialog = false
                                    },
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(displayName, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        parsed.providerName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showCompactionModelDialog = false }) {
                        Text(stringResource(R.string.provider_cancel))
                    }
                }
            )
        }

        // Keep-recent window (only meaningful for deterministic text summaries — under LLM
        // summarization the model decides the fold, so the option is hidden).
        if (effectiveSummaryMode != d.COMPACTION_SUMMARY_LLM) {
            CompactionIntSlider(
                label = stringResource(R.string.compaction_keep_recent),
                value = effectiveKeepRecent,
                range = 1..20,
                onChange = onKeepRecent
            )
        }

        // Limit mode (auto / manual)
        Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Text(
                text = stringResource(R.string.compaction_limit_mode),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(6.dp))
            PillTabSwitcher(
                tabs = listOf(
                    stringResource(R.string.compaction_limit_auto),
                    stringResource(R.string.compaction_limit_manual)
                ),
                selectedIndex = if (effectiveLimitMode == d.COMPACTION_LIMIT_MANUAL) 1 else 0,
                onSelect = { index ->
                    onLimitMode(if (index == 1) d.COMPACTION_LIMIT_MANUAL else d.COMPACTION_LIMIT_AUTO)
                }
            )
        }

        if (effectiveLimitMode == d.COMPACTION_LIMIT_MANUAL) {
            CompactionIntSlider(
                label = stringResource(R.string.compaction_limit_manual_tokens),
                value = effectiveManualTokens,
                range = 1000..16000,
                step = 250,
                onChange = onManualTokens
            )
        }
    }
}

/**
 * Compact integer slider row for the Advanced dialog's compaction section.
 */
@Composable
private fun CompactionIntSlider(
    label: String,
    value: Int,
    range: IntRange,
    percent: Boolean = false,
    step: Int = 1,
    onChange: (Int) -> Unit
) {
    var draft by remember { mutableFloatStateOf(value.toFloat()) }
    LaunchedEffect(value) { draft = value.toFloat() }
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = if (percent) "${draft.roundToInt()}%" else draft.roundToInt().toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
        Slider(
            value = draft,
            onValueChange = { draft = it },
            onValueChangeFinished = {
                val committed = draft.roundToInt().coerceIn(range.first, range.last)
                draft = committed.toFloat()
                onChange(committed)
            },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = ((range.last - range.first) / step) - 1,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
