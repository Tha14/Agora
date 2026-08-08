package com.newoether.agora.viewmodel

import com.newoether.agora.data.CompactionConfig
import com.newoether.agora.data.SettingsManager
import com.newoether.agora.model.ChatMessage
import com.newoether.agora.model.MessageStatus
import com.newoether.agora.model.Participant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CompactionTokenEstimatorTest {

    private fun message(
        id: String = "m",
        text: String,
        participant: Participant = Participant.USER,
        images: List<String> = emptyList(),
        thoughts: String? = null,
    ) = ChatMessage(
        id = id,
        parentId = null,
        text = text,
        images = images,
        thoughts = thoughts,
        participant = participant,
        status = MessageStatus.SUCCESS,
        timestamp = 0L,
    )

    @Test
    fun plainText_estimates_charLengthDividedByFour() {
        val text = "a".repeat(40)
        assertEquals(10 + CompactionTokenEstimator.PER_MESSAGE_OVERHEAD, CompactionTokenEstimator.estimateText(text))
    }

    @Test
    fun perMessageOverhead_isAlwaysApplied() {
        assertEquals(
            CompactionTokenEstimator.PER_MESSAGE_OVERHEAD.toLong(),
            CompactionTokenEstimator.estimate(message(text = "")).toLong(),
        )
    }

    @Test
    fun images_addFlatCost() {
        val withImages = CompactionTokenEstimator.estimate(message(text = "", images = listOf("a", "b")))
        val without = CompactionTokenEstimator.estimate(message(text = ""))
        assertEquals((CompactionTokenEstimator.PER_IMAGE_TOKENS * 2).toLong(), (withImages - without).toLong())
    }

    @Test
    fun thoughtsAndSegments_areCounted() {
        val withThoughts = CompactionTokenEstimator.estimate(
            message(text = "x", thoughts = "y".repeat(40)),
        )
        val without = CompactionTokenEstimator.estimate(message(text = "x"))
        assertTrue(withThoughts > without)
    }

    @Test
    fun listEstimate_isSumOfParts() {
        val messages = listOf(
            message("a", "hello"),
            message("b", "world"),
            message("c", "again", participant = Participant.MODEL),
        )
        assertEquals(
            messages.sumOf { CompactionTokenEstimator.estimate(it) }.toLong(),
            CompactionTokenEstimator.estimate(messages).toLong(),
        )
    }
}

class CompactionFoldBoundaryTest {

    private val config: CompactionConfig = CompactionConfig(
        enabled = true,
        strategy = SettingsManager.COMPACTION_STRATEGY_TOKEN_PERCENT,
        messageCount = 40,
        tokenPercent = 80,
        tokenSize = 0,
        summaryMode = SettingsManager.COMPACTION_SUMMARY_DETERMINISTIC,
        llmModel = null,
        keepRecent = 4,
        limitMode = SettingsManager.COMPACTION_LIMIT_AUTO,
        manualContextTokens = 4096,
    )

    @Test
    fun underBudget_doesNotFold() {
        assertEquals(0, computeFoldBoundary(config, messageCount = 8, estimatedTokens = 1000, budgetContext = 32_768))
    }

    @Test
    fun overBudget_foldsNonZeroButKeepsRecentMargin() {
        // effective budget = 8000 * 80% = 6400 tokens; 16 messages * 800 tokens = 12800 > budget
        val fold = computeFoldBoundary(
            config,
            messageCount = 16,
            estimatedTokens = 16 * 800,
            budgetContext = 8000,
        )
        // fold = excess(6400) / avgPerMessage(800) = 8; bounded by 16 - keepRecent(4) = 12
        assertTrue(fold in 1..12)
        assertEquals(8, fold)
    }

    @Test
    fun messageCountStrategy_keepsTargetAndRecentMargin() {
        val cfg = config.copy(strategy = SettingsManager.COMPACTION_STRATEGY_MESSAGE_COUNT, messageCount = 10)
        val fold = computeFoldBoundary(cfg, messageCount = 16, estimatedTokens = 16 * 800, budgetContext = 32_768)
        assertEquals(6, fold) // 16 - 10
    }

    @Test
    fun tinyConversation_neverFoldsBelowOneMessage() {
        assertEquals(0, computeFoldBoundary(config, messageCount = 1, estimatedTokens = 1_000_000, budgetContext = 32_768))
    }

    @Test
    fun effectiveTokenSize_fallsBackToContextPercent() {
        val c = config.copy(tokenSize = 0, tokenPercent = 50)
        assertEquals(16_384, c.effectiveTokenSize(32_768))
    }
}