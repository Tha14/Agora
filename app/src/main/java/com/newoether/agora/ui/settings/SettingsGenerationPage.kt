package com.newoether.agora.ui.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Speed
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.newoether.agora.R
import com.newoether.agora.model.apiModelName
import com.newoether.agora.ui.common.OpenAiServiceTierControlPanel
import com.newoether.agora.ui.common.ThinkingControlPanel
import com.newoether.agora.ui.common.openAiServiceTierShortLabel
import com.newoether.agora.ui.common.thinkingControlShortLabel
import com.newoether.agora.viewmodel.ChatViewModel
import kotlin.math.roundToInt
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsGenerationPage(viewModel: ChatViewModel, onBack: () -> Unit) {
    val maxContextWindow by viewModel.settings.maxContextWindow.collectAsState()
    val visualizeContextRollout by viewModel.settings.visualizeContextRollout.collectAsState()
    val defaultTemperature by viewModel.settings.defaultTemperature.collectAsState()
    val defaultMaxTokens by viewModel.settings.defaultMaxTokens.collectAsState()
    val defaultTopP by viewModel.settings.defaultTopP.collectAsState()
    val defaultFrequencyPenalty by viewModel.settings.defaultFrequencyPenalty.collectAsState()
    val defaultPresencePenalty by viewModel.settings.defaultPresencePenalty.collectAsState()
    val thinkingEnabled by viewModel.settings.thinkingEnabled.collectAsState()
    val thinkingLevel by viewModel.settings.thinkingLevel.collectAsState()
    val thinkingBudgetEnabled by viewModel.settings.thinkingBudgetEnabled.collectAsState()
    val thinkingBudgetTokens by viewModel.settings.thinkingBudgetTokens.collectAsState()
    val openAiServiceTierEnabled by
        viewModel.settings.openAiServiceTierEnabled.collectAsState()
    val openAiServiceTier by viewModel.settings.openAiServiceTier.collectAsState()
    val showDocFab by viewModel.settings.showDocumentationFab.collectAsState()
    val compactEnabled by viewModel.settings.compactionEnabled.collectAsState()
    val compactStrategy by viewModel.settings.compactionStrategy.collectAsState()
    val compactMessageCount by viewModel.settings.compactionMessageCount.collectAsState()
    val compactTokenPercent by viewModel.settings.compactionTokenPercent.collectAsState()
    val compactTokenSize by viewModel.settings.compactionTokenSize.collectAsState()
    val compactSummaryMode by viewModel.settings.compactionSummaryMode.collectAsState()
    val compactLlmModel by viewModel.settings.compactionLlmModel.collectAsState()
    val compactKeepRecent by viewModel.settings.compactionKeepRecent.collectAsState()
    val compactLimitMode by viewModel.settings.compactionLimitMode.collectAsState()
    val manualContextTokens by viewModel.settings.manualContextTokens.collectAsState()
    val modelAliases by viewModel.settings.modelAliases.collectAsState()
    val enabledModels by viewModel.settings.enabledModels.collectAsState()
    var showCompactModelDialog by remember { mutableStateOf(false) }
    val compactSummaryInstructions by viewModel.settings.compactionSummaryInstructions.collectAsState()
    var showCompactionInstructionsDialog by remember { mutableStateOf(false) }

    CollapsingSettingsScaffold(
        title = stringResource(R.string.generation_title),
        onBack = onBack,
        floatingActionButton = { if (showDocFab) DocumentationFab("generation.md") }
    ) {
            SettingsGroupColumn {
                // ── Section 1: Default Context Window ──
                SettingsGroup(
                    title = stringResource(R.string.context_window_default),
                    items = listOf(
                        {
                            val persistedContextWindow = maxContextWindow.toFloat()
                            var contextWindowDraft by remember { mutableFloatStateOf(persistedContextWindow) }
                            LaunchedEffect(persistedContextWindow) {
                                contextWindowDraft = persistedContextWindow
                            }
                            val contextWindowValue = contextWindowDraft.toInt().coerceIn(5, 100)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        Icons.Default.Memory,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(R.string.context_window),
                                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = stringResource(R.string.context_retain, contextWindowValue),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                        Slider(
                                            value = contextWindowDraft,
                                            onValueChange = { contextWindowDraft = it },
                                            onValueChangeFinished = {
                                                val committed = contextWindowDraft.toInt().coerceIn(5, 100)
                                                contextWindowDraft = committed.toFloat()
                                                if (committed != maxContextWindow) {
                                                    viewModel.settings.setMaxContextWindow(committed)
                                                }
                                            },
                                            valueRange = 5f..100f,
                                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                    }
                                }
                            }
                        },
                        {
                            SettingsItem(
                                headlineContent = { Text(stringResource(R.string.context_visualize)) },
                                supportingContent = { Text(stringResource(R.string.context_visualize_desc)) },
                                leadingContent = {
                                    Icon(Icons.Default.Visibility, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                },
                                trailingContent = {
                                    Switch(checked = visualizeContextRollout, onCheckedChange = { viewModel.settings.setVisualizeContextRollout(it) })
                                },
                                modifier = Modifier.clickable { viewModel.settings.setVisualizeContextRollout(!visualizeContextRollout) }
                            )
                        }
                    )
                )

                // ── Section 2: Default Thinking ──
                SettingsGroup(
                    title = stringResource(R.string.default_thinking),
                    items = listOf(
                        {
                            SettingsItem(
                                headlineContent = { Text(stringResource(R.string.gen_thinking_enabled)) },
                                supportingContent = {
                                    Text(thinkingControlShortLabel(thinkingEnabled, thinkingLevel, thinkingBudgetEnabled, thinkingBudgetTokens))
                                },
                                leadingContent = {
                                    Icon(painterResource(id = R.drawable.neurology_24), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                },
                                trailingContent = {
                                    Switch(checked = thinkingEnabled, onCheckedChange = { viewModel.settings.setThinkingEnabled(it) })
                                },
                                modifier = Modifier.clickable { viewModel.settings.setThinkingEnabled(!thinkingEnabled) }
                            )
                        },
                        {
                            ThinkingControlPanel(
                                enabled = thinkingEnabled,
                                level = thinkingLevel,
                                budgetEnabled = thinkingBudgetEnabled,
                                budgetTokens = thinkingBudgetTokens,
                                onEnabledChange = { viewModel.settings.setThinkingEnabled(it) },
                                onLevelChange = { viewModel.settings.setThinkingLevel(it) },
                                onBudgetEnabledChange = { viewModel.settings.setThinkingBudgetEnabled(it) },
                                onBudgetTokensChange = { viewModel.settings.setThinkingBudgetTokens(it) },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                                showHeader = false,
                                providerName = null,
                                animateSections = true
                            )
                        }
                    )
                )

                // ── Section 3: Default OpenAI service tier ──
                SettingsGroup(
                    title = stringResource(R.string.default_service_tier),
                    items = listOf(
                        {
                            SettingsItem(
                                headlineContent = {
                                    Text(stringResource(R.string.openai_service_tier_title))
                                },
                                supportingContent = {
                                    Text(
                                        openAiServiceTierShortLabel(
                                            openAiServiceTierEnabled,
                                            openAiServiceTier,
                                        )
                                    )
                                },
                                leadingContent = {
                                    Icon(
                                        Icons.Default.Speed,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                },
                                trailingContent = {
                                    Switch(
                                        checked = openAiServiceTierEnabled,
                                        onCheckedChange =
                                            viewModel.settings::setOpenAiServiceTierEnabled,
                                    )
                                },
                                modifier = Modifier.clickable {
                                    viewModel.settings.setOpenAiServiceTierEnabled(
                                        !openAiServiceTierEnabled,
                                    )
                                },
                            )
                        },
                        {
                            OpenAiServiceTierControlPanel(
                                enabled = openAiServiceTierEnabled,
                                tier = openAiServiceTier,
                                onEnabledChange =
                                    viewModel.settings::setOpenAiServiceTierEnabled,
                                onTierChange = viewModel.settings::setOpenAiServiceTier,
                                modifier = Modifier.padding(
                                    horizontal = 16.dp,
                                    vertical = 16.dp,
                                ),
                                showHeader = false,
                            )
                        },
                    ),
                )

                // ── Section 4: Generation Parameters ──
                SettingsGroup(
                    title = stringResource(R.string.generation_params),
                    items = listOf(
                        {
                            GenParamSlider(
                                label = stringResource(R.string.gen_temperature),
                                desc = stringResource(R.string.gen_temperature_desc),
                                value = defaultTemperature,
                                valueRange = 0f..2f,
                                format = { v -> String.format(Locale.US, "%.2f", v) },
                                onValueChange = { viewModel.settings.setDefaultTemperature(it) },
                                onReset = { viewModel.settings.setDefaultTemperature(null) }
                            )
                        },
                        {
                            val maxTokensPresets = intArrayOf(256, 512, 1024, 2048, 4096, 8192, 16384, 32768)
                            GenParamSlider(
                                label = stringResource(R.string.gen_max_tokens),
                                desc = stringResource(R.string.gen_max_tokens_desc),
                                value = defaultMaxTokens,
                                presets = maxTokensPresets,
                                format = { it.toString() },
                                onValueChange = { viewModel.settings.setDefaultMaxTokens(it) },
                                onReset = { viewModel.settings.setDefaultMaxTokens(null) }
                            )
                        },
                        {
                            GenParamSlider(
                                label = stringResource(R.string.gen_top_p),
                                desc = stringResource(R.string.gen_top_p_desc),
                                value = defaultTopP,
                                valueRange = 0f..1f,
                                format = { v -> String.format(Locale.US, "%.2f", v) },
                                onValueChange = { viewModel.settings.setDefaultTopP(it) },
                                onReset = { viewModel.settings.setDefaultTopP(null) }
                            )
                        },
                        {
                            GenParamSlider(
                                label = stringResource(R.string.gen_frequency_penalty),
                                desc = stringResource(R.string.gen_frequency_penalty_desc),
                                value = defaultFrequencyPenalty,
                                valueRange = -2f..2f,
                                format = { v -> String.format(Locale.US, "%.2f", v) },
                                onValueChange = { viewModel.settings.setDefaultFrequencyPenalty(it) },
                                onReset = { viewModel.settings.setDefaultFrequencyPenalty(null) }
                            )
                        },
                        {
                            GenParamSlider(
                                label = stringResource(R.string.gen_presence_penalty),
                                desc = stringResource(R.string.gen_presence_penalty_desc),
                                value = defaultPresencePenalty,
                                valueRange = -2f..2f,
                                format = { v -> String.format(Locale.US, "%.2f", v) },
                                onValueChange = { viewModel.settings.setDefaultPresencePenalty(it) },
                                onReset = { viewModel.settings.setDefaultPresencePenalty(null) }
                            )
                        }
                    )
                )

                // ── Section 5: Context Compaction ──
                SettingsGroup(
                    title = stringResource(R.string.compaction_title),
                    items = buildList {
                        add {
                            SettingsItem(
                                headlineContent = { Text(stringResource(R.string.compaction_enabled)) },
                                supportingContent = { Text(stringResource(R.string.compaction_enabled_desc)) },
                                leadingContent = {
                                    Icon(
                                        painterResource(id = R.drawable.neurology_24),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                },
                                trailingContent = {
                                    Switch(
                                        checked = compactEnabled,
                                        onCheckedChange = viewModel.settings::setCompactionEnabled,
                                    )
                                },
                                modifier = Modifier.clickable {
                                    viewModel.settings.setCompactionEnabled(!compactEnabled)
                                },
                            )
                        }
                        if (compactEnabled) {
                            add {
                                CompactionStrategyPanel(
                                    strategy = compactStrategy,
                                    messageCount = compactMessageCount,
                                    tokenPercent = compactTokenPercent,
                                    tokenSize = compactTokenSize,
                                    onStrategyChange = viewModel.settings::setCompactionStrategy,
                                    onMessageCountChange = viewModel.settings::setCompactionMessageCount,
                                    onTokenPercentChange = viewModel.settings::setCompactionTokenPercent,
                                    onTokenSizeChange = viewModel.settings::setCompactionTokenSize,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                                )
                            }
                            add {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 16.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.compaction_summary_mode),
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    PillTabSwitcher(
                                        tabs = listOf(
                                            stringResource(R.string.compaction_summary_llm),
                                            stringResource(R.string.compaction_summary_deterministic)
                                        ),
                                        selectedIndex = if (compactSummaryMode == com.newoether.agora.data.SettingsManager.COMPACTION_SUMMARY_LLM) 0 else 1,
                                        onSelect = { index ->
                                            viewModel.settings.setCompactionSummaryMode(
                                                if (index == 0) com.newoether.agora.data.SettingsManager.COMPACTION_SUMMARY_LLM
                                                else com.newoether.agora.data.SettingsManager.COMPACTION_SUMMARY_DETERMINISTIC
                                            )
                                        }
                                    )
                                }
                            }
                            if (compactSummaryMode == com.newoether.agora.data.SettingsManager.COMPACTION_SUMMARY_LLM) {
                                add {
                                    val displayName = if (compactLlmModel == null) {
                                        stringResource(R.string.compaction_model_current)
                                    } else {
                                        val model = compactLlmModel ?: ""
                                        val alias = modelAliases[model]
                                        alias ?: com.newoether.agora.model.ModelId.parse(model).apiModelName
                                    }
                                    SettingsItem(
                                        headlineContent = { Text(stringResource(R.string.compaction_model)) },
                                        supportingContent = { Text(displayName) },
                                        leadingContent = {
                                            Icon(
                                                Icons.Default.Chat,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                            )
                                        },
                                        modifier = Modifier.clickable { showCompactModelDialog = true },
                                    )
                                }
                                add {
                                    SettingsItem(
                                        headlineContent = { Text(stringResource(R.string.compaction_message)) },
                                        supportingContent = {
                                            Text(
                                                if (compactSummaryInstructions.isBlank()) {
                                                    stringResource(R.string.compaction_message_default)
                                                } else {
                                                    compactSummaryInstructions
                                                },
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        },
                                        leadingContent = {
                                            Icon(
                                                Icons.Default.Tune,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                            )
                                        },
                                        modifier = Modifier.clickable { showCompactionInstructionsDialog = true },
                                    )
                                }
                            }
                            // "Keep recent messages" describes how many newest messages stay
                            // verbatim when a deterministic name-summary prefaces the tail. Under
                            // LLM summarization the model decides the fold, so the option is hidden.
                            if (compactSummaryMode != com.newoether.agora.data.SettingsManager.COMPACTION_SUMMARY_LLM) {
                                add {
                                    CompactionKeepRecentSlider(
                                        keepRecent = compactKeepRecent,
                                        onChanged = viewModel.settings::setCompactionKeepRecent,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                                    )
                                }
                            }
                            add {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 16.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.compaction_limit_mode),
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    PillTabSwitcher(
                                        tabs = listOf(
                                            stringResource(R.string.compaction_limit_auto),
                                            stringResource(R.string.compaction_limit_manual)
                                        ),
                                        selectedIndex = if (compactLimitMode == com.newoether.agora.data.SettingsManager.COMPACTION_LIMIT_MANUAL) 1 else 0,
                                        onSelect = { index ->
                                            viewModel.settings.setCompactionLimitMode(
                                                if (index == 1) com.newoether.agora.data.SettingsManager.COMPACTION_LIMIT_MANUAL
                                                else com.newoether.agora.data.SettingsManager.COMPACTION_LIMIT_AUTO
                                            )
                                        }
                                    )
                                }
                            }
                            if (compactLimitMode == com.newoether.agora.data.SettingsManager.COMPACTION_LIMIT_MANUAL) {
                                add {
                                    ManualContextSlider(
                                        tokens = manualContextTokens,
                                        onTokensChange = viewModel.settings::setManualContextTokens,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                                    )
                                }
                            }
                        }
                    },
                )
            }

            if (showDocFab) { Spacer(modifier = Modifier.height(80.dp)) }
    }

    if (showCompactModelDialog) {
        val enabledModelsList = enabledModels.toList()
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            onDismissRequest = { showCompactModelDialog = false },
            title = { Text(stringResource(R.string.compaction_model), fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    item {
                        SettingsItem(
                            headlineContent = {
                                Text(
                                    stringResource(R.string.compaction_model_current),
                                    fontWeight = if (compactLlmModel == null) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            supportingContent = {
                                Text(
                                    stringResource(R.string.compaction_model_current_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            },
                            leadingContent = {
                                RadioButton(selected = compactLlmModel == null, onClick = {
                                    viewModel.settings.setCompactionLlmModel(null)
                                    showCompactModelDialog = false
                                })
                            },
                            modifier = Modifier.clickable {
                                viewModel.settings.setCompactionLlmModel(null)
                                showCompactModelDialog = false
                            }
                        )
                    }
                    items(enabledModelsList, key = { it }) { model ->
                        val alias = modelAliases[model]
                        val parsed = com.newoether.agora.model.ModelId.parse(model)
                        val displayName = alias ?: parsed.apiModelName
                        SettingsItem(
                            headlineContent = {
                                Text(
                                    displayName,
                                    fontWeight = if (compactLlmModel == model) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            supportingContent = {
                                Text(
                                    parsed.providerName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            },
                            leadingContent = {
                                RadioButton(selected = compactLlmModel == model, onClick = {
                                    viewModel.settings.setCompactionLlmModel(model)
                                    showCompactModelDialog = false
                                })
                            },
                            modifier = Modifier.clickable {
                                viewModel.settings.setCompactionLlmModel(model)
                                showCompactModelDialog = false
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCompactModelDialog = false }) {
                    Text(stringResource(R.string.provider_cancel))
                }
            }
        )
    }

    if (showCompactionInstructionsDialog) {
        var draft by remember(compactSummaryInstructions) { mutableStateOf(compactSummaryInstructions) }
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            onDismissRequest = { showCompactionInstructionsDialog = false },
            title = { Text(stringResource(R.string.compaction_message), fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        stringResource(R.string.compaction_message_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp),
                        placeholder = { Text(stringResource(R.string.compaction_message_default)) },
                    )
                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = {
                        viewModel.settings.setCompactionSummaryInstructions("")
                        showCompactionInstructionsDialog = false
                    }) {
                        Text(stringResource(R.string.gen_reset))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.settings.setCompactionSummaryInstructions(draft)
                    showCompactionInstructionsDialog = false
                }) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCompactionInstructionsDialog = false }) {
                    Text(stringResource(R.string.provider_cancel))
                }
            }
        )
    }
}

/**
 * Context compaction strategy panel: lets the user pick how context is compacted
 * (by message count / token percent / absolute token size) and adjust the threshold.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactionStrategyPanel(
    strategy: String,
    messageCount: Int?,
    tokenPercent: Int?,
    tokenSize: Int?,
    onStrategyChange: (String) -> Unit,
    onMessageCountChange: (Int) -> Unit,
    onTokenPercentChange: (Int) -> Unit,
    onTokenSizeChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.compaction_strategy_label),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(12.dp))
        PillTabSwitcher(
            tabs = listOf(
                stringResource(R.string.compaction_strategy_messages_short),
                stringResource(R.string.compaction_strategy_percent_short),
                stringResource(R.string.compaction_strategy_tokens_short)
            ),
            selectedIndex = when (strategy) {
                com.newoether.agora.data.SettingsManager.COMPACTION_STRATEGY_MESSAGE_COUNT -> 0
                com.newoether.agora.data.SettingsManager.COMPACTION_STRATEGY_TOKEN_PERCENT -> 1
                else -> 2
            },
            onSelect = { index ->
                onStrategyChange(
                    when (index) {
                        0 -> com.newoether.agora.data.SettingsManager.COMPACTION_STRATEGY_MESSAGE_COUNT
                        1 -> com.newoether.agora.data.SettingsManager.COMPACTION_STRATEGY_TOKEN_PERCENT
                        else -> com.newoether.agora.data.SettingsManager.COMPACTION_STRATEGY_TOKEN_SIZE
                    }
                )
            }
        )
        Spacer(Modifier.height(16.dp))
        when (strategy) {
            com.newoether.agora.data.SettingsManager.COMPACTION_STRATEGY_MESSAGE_COUNT -> {
                val base = messageCount ?: 12
                val validated = base.coerceIn(4, 64)
                ThresholdSlider(
                    label = stringResource(R.string.compaction_strategy_messages_value),
                    value = validated,
                    valueRange = 4..64,
                    onChanged = onMessageCountChange
                )
            }
            com.newoether.agora.data.SettingsManager.COMPACTION_STRATEGY_TOKEN_PERCENT -> {
                val base = tokenPercent ?: 75
                val validated = base.coerceIn(30, 90)
                ThresholdSlider(
                    label = stringResource(R.string.compaction_strategy_percent_value),
                    value = validated,
                    valueRange = 30..90,
                    percent = true,
                    onChanged = onTokenPercentChange
                )
            }
            else -> {
                val base = tokenSize ?: 8000
                val validated = base.coerceIn(1000, 32000)
                ThresholdSlider(
                    label = stringResource(R.string.compaction_strategy_tokens_value),
                    value = validated,
                    valueRange = 1000..32000,
                    step = 1000,
                    onChanged = onTokenSizeChange
                )
            }
        }
    }
}

/**
 * Slider row that commits on release; shows the numeric value on the right.
 */
@Composable
private fun ThresholdSlider(
    label: String,
    value: Int,
    valueRange: IntRange,
    percent: Boolean = false,
    step: Int = 1,
    onChanged: (Int) -> Unit
) {
    val sliderRange = valueRange.first.toFloat()..valueRange.last.toFloat()
    var draft by remember { mutableFloatStateOf(value.toFloat()) }
    LaunchedEffect(value) { draft = value.toFloat() }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = if (percent) "$draft%" else draft.roundToInt().toString(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
        Slider(
            value = draft,
            onValueChange = { draft = it },
            onValueChangeFinished = {
                val committed = draft.roundToInt().coerceIn(valueRange.first, valueRange.last)
                draft = committed.toFloat()
                onChanged(committed)
            },
            valueRange = sliderRange,
            steps = (valueRange.last - valueRange.first) / step - 1,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        )
    }
}

/**
 * How many most-recent messages to always keep in the forwarded context
 * when compaction triggers. Higher = larger retained window.
 */
@Composable
private fun CompactionKeepRecentSlider(
    keepRecent: Int?,
    onChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val base = keepRecent ?: 4
    var draftIdx by remember { mutableFloatStateOf(base.coerceIn(1, 20).toFloat()) }
    LaunchedEffect(base) { draftIdx = base.coerceIn(1, 20).toFloat() }
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.compaction_keep_recent),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = draftIdx.roundToInt().toString(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
        Slider(
            value = draftIdx,
            onValueChange = { draftIdx = it },
            onValueChangeFinished = {
                val committed = draftIdx.roundToInt().coerceIn(1, 20)
                draftIdx = committed.toFloat()
                onChanged(committed)
            },
            valueRange = 1f..20f,
            steps = 18,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Manual hard limit on forwarded context tokens, used when limit mode = manual.
 */
@Composable
private fun ManualContextSlider(
    tokens: Int?,
    onTokensChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val base = tokens ?: 6000
    val sliderRange = 1000..16000
    var draft by remember { mutableFloatStateOf(base.toFloat()) }
    LaunchedEffect(base) { draft = base.toFloat() }
    fun formatEntry(v: Int): String = when {
        v >= 10000 -> "%.1fK".format(Locale.US, v / 1000.0)
        v >= 1000 -> "%.0fK".format(Locale.US, v / 1000.0)
        else -> v.toString()
    }
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.compaction_limit_manual_tokens),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.compaction_limit_manual_tokens_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = formatEntry(draft.roundToInt()),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
        Slider(
            value = draft,
            onValueChange = { draft = it },
            onValueChangeFinished = {
                val committed = draft.roundToInt().coerceIn(sliderRange.first, sliderRange.last)
                draft = committed.toFloat()
                onTokensChange(committed)
            },
            valueRange = sliderRange.first.toFloat()..sliderRange.last.toFloat(),
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        )
    }
}

/**
 * Generation parameter slider row.
 * Always shows the slider value. When at default, value is grey and "Default" text is shown beside it.
 * When set, value is primary-colored with a "Reset" link below the slider.
 */
@Composable
private fun GenParamSlider(
    label: String,
    desc: String,
    value: Float?,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    format: (Float) -> String,
    onValueChange: (Float) -> Unit,
    onReset: () -> Unit
) {
    val defaultSliderPos = (valueRange.start + valueRange.endInclusive) / 2f
    val persistedSliderPos = value ?: defaultSliderPos
    var sliderPos by remember { mutableFloatStateOf(persistedSliderPos) }
    LaunchedEffect(persistedSliderPos) {
        sliderPos = persistedSliderPos
    }
    // Reset is reflected synchronously; only the DataStore write is async. justReset
    // flips the label to "not specified" immediately and is cleared once the async
    // [value] catches up (becomes null on reset, or a new value if the user re-sets).
    var justReset by remember { mutableStateOf(false) }
    LaunchedEffect(value) { justReset = false }
    val draftChangedFromDefault = kotlin.math.abs(sliderPos - defaultSliderPos) > 0.0001f
    val hasExplicitOrDraftValue = (value != null && !justReset) || draftChangedFromDefault
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Default.Tune,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    if (!hasExplicitOrDraftValue) {
                        Text(
                            text = stringResource(R.string.gen_not_specified),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    } else {
                        Text(
                            text = format(sliderPos),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = stringResource(R.string.gen_reset),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            modifier = Modifier.clickable {
                                sliderPos = defaultSliderPos
                                justReset = true
                                onReset()
                            }
                        )
                    }
                }
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Slider(
                    value = sliderPos,
                    onValueChange = { sliderPos = it },
                    onValueChangeFinished = {
                        val committed = sliderPos.coerceIn(valueRange.start, valueRange.endInclusive)
                        val shouldCommit = value != null || kotlin.math.abs(committed - defaultSliderPos) > 0.0001f
                        sliderPos = committed
                        if (shouldCommit) {
                            if (value == null || kotlin.math.abs(value - committed) > 0.0001f) {
                                onValueChange(committed)
                            }
                        }
                    },
                    valueRange = valueRange,
                    steps = steps,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }
        }
    }
}

/** Int slider variant with discrete preset values (used for max tokens). */
@Composable
private fun GenParamSlider(
    label: String,
    desc: String,
    value: Int?,
    presets: IntArray,
    format: (Int) -> String,
    onValueChange: (Int) -> Unit,
    onReset: () -> Unit
) {
    fun toIndex(v: Int) = presets.indices.minByOrNull { kotlin.math.abs(presets[it] - v) } ?: 3
    val defaultIndex = 3.coerceIn(0, presets.lastIndex)
    val persistedIndex = if (value != null) toIndex(value) else defaultIndex
    var sliderPos by remember { mutableFloatStateOf(persistedIndex.toFloat()) }
    LaunchedEffect(persistedIndex) {
        sliderPos = persistedIndex.toFloat()
    }
    // Reset is reflected synchronously; only the DataStore write is async (see float variant).
    var justReset by remember { mutableStateOf(false) }
    LaunchedEffect(value) { justReset = false }
    val draftIndex = sliderPos.roundToInt().coerceIn(0, presets.lastIndex)
    val hasExplicitOrDraftValue = (value != null && !justReset) || draftIndex != defaultIndex
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Default.Tune,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    if (!hasExplicitOrDraftValue) {
                        Text(
                            text = stringResource(R.string.gen_not_specified),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    } else {
                        Text(
                            text = format(presets[draftIndex]),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = stringResource(R.string.gen_reset),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            modifier = Modifier.clickable {
                                sliderPos = defaultIndex.toFloat()
                                justReset = true
                                onReset()
                            }
                        )
                    }
                }
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Slider(
                    value = sliderPos,
                    onValueChange = { sliderPos = it },
                    onValueChangeFinished = {
                        val committedIndex = sliderPos.roundToInt().coerceIn(0, presets.lastIndex)
                        val committedValue = presets[committedIndex]
                        val shouldCommit = value != null || committedIndex != defaultIndex
                        sliderPos = committedIndex.toFloat()
                        if (shouldCommit) {
                            if (value != committedValue) {
                                onValueChange(committedValue)
                            }
                        }
                    },
                    valueRange = 0f..(presets.size - 1).toFloat(),
                    steps = presets.size - 2,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }
        }
    }
}
