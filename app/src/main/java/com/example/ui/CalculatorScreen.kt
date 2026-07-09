package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HistoryToggleOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HistoryItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
    viewModel: CalculatorViewModel,
    modifier: Modifier = Modifier
) {
    val expression by viewModel.expression.collectAsState()
    val previewResult by viewModel.previewResult.collectAsState()
    val history by viewModel.history.collectAsState()
    val isHistoryExpanded by viewModel.isHistoryExpanded.collectAsState()

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val copyToClipboard: (String) -> Unit = { text ->
        if (text.isNotEmpty() && text != "Error") {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Calculation Result", text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "Copied: $text", Toast.LENGTH_SHORT).show()
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Calculator",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                actions = {
                    if (isHistoryExpanded && history.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                viewModel.onClearHistoryClick()
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            modifier = Modifier.testTag("clear_history_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear History",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            viewModel.onToggleHistoryExpanded()
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        modifier = Modifier.testTag("toggle_history_button")
                    ) {
                        Icon(
                            imageVector = if (isHistoryExpanded) Icons.Default.HistoryToggleOff else Icons.Default.History,
                            contentDescription = if (isHistoryExpanded) "Close History" else "View History",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // History section (Collapsible)
            AnimatedVisibility(
                visible = isHistoryExpanded,
                enter = expandVertically(animationSpec = spring()),
                exit = shrinkVertically(animationSpec = spring())
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    if (history.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No calculations yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            reverseLayout = false,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(history) { item ->
                                HistoryRowItem(
                                    item = item,
                                    onClick = {
                                        viewModel.onHistoryItemClick(item)
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Display area (Input & Preview)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .clickable(enabled = expression.isNotEmpty()) {
                        copyToClipboard(expression)
                    },
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.End
            ) {
                val scrollState = rememberScrollState()

                // Expression text
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState, reverseScrolling = true),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = expression.ifEmpty { "0" },
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = if (expression.length > 10) 36.sp else 48.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        modifier = Modifier.testTag("expression_display")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Live Preview Result text
                Text(
                    text = previewResult,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .testTag("preview_display")
                        .clickable(enabled = previewResult.isNotEmpty()) {
                            // Copy parsed result (stripping the "= " prefix)
                            copyToClipboard(previewResult.replace("= ", "").trim())
                        }
                )
            }

            // Keyboard area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                    .padding(bottom = 16.dp, top = 8.dp)
            ) {
                // Horizontal scrollable Advanced Row (parentheses, constants)
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        AdvancedKey(
                            label = "(",
                            onClick = {
                                viewModel.onParenthesisClick("(")
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            tag = "btn_paren_open"
                        )
                    }
                    item {
                        AdvancedKey(
                            label = ")",
                            onClick = {
                                viewModel.onParenthesisClick(")")
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            tag = "btn_paren_close"
                        )
                    }
                    item {
                        AdvancedKey(
                            label = "π",
                            onClick = {
                                viewModel.onDigitClick("3.14159265")
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            tag = "btn_pi"
                        )
                    }
                    item {
                        AdvancedKey(
                            label = "e",
                            onClick = {
                                viewModel.onDigitClick("2.71828182")
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            tag = "btn_e"
                        )
                    }
                }

                // Core Keypad
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    val isWideScreen = maxWidth > 600.dp
                    val containerModifier = if (isWideScreen) {
                        Modifier
                            .widthIn(max = 480.dp)
                            .align(Alignment.Center)
                    } else {
                        Modifier.fillMaxWidth()
                    }

                    Column(
                        modifier = containerModifier,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Row 1: AC, +/-, %, ÷
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CalculatorButton(
                                label = "AC",
                                category = ButtonCategory.Action,
                                onClick = { viewModel.onClearClick() },
                                modifier = Modifier.weight(1f),
                                tag = "btn_clear"
                            )
                            CalculatorButton(
                                label = "+/-",
                                category = ButtonCategory.Action,
                                onClick = { viewModel.onToggleSignClick() },
                                modifier = Modifier.weight(1f),
                                tag = "btn_toggle_sign"
                            )
                            CalculatorButton(
                                label = "%",
                                category = ButtonCategory.Action,
                                onClick = { viewModel.onPercentClick() },
                                modifier = Modifier.weight(1f),
                                tag = "btn_percent"
                            )
                            CalculatorButton(
                                label = "÷",
                                category = ButtonCategory.Operator,
                                onClick = { viewModel.onOperatorClick("÷") },
                                modifier = Modifier.weight(1f),
                                tag = "btn_divide"
                            )
                        }

                        // Row 2: 7, 8, 9, ×
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CalculatorButton(
                                label = "7",
                                category = ButtonCategory.Numeric,
                                onClick = { viewModel.onDigitClick("7") },
                                modifier = Modifier.weight(1f),
                                tag = "btn_7"
                            )
                            CalculatorButton(
                                label = "8",
                                category = ButtonCategory.Numeric,
                                onClick = { viewModel.onDigitClick("8") },
                                modifier = Modifier.weight(1f),
                                tag = "btn_8"
                            )
                            CalculatorButton(
                                label = "9",
                                category = ButtonCategory.Numeric,
                                onClick = { viewModel.onDigitClick("9") },
                                modifier = Modifier.weight(1f),
                                tag = "btn_9"
                            )
                            CalculatorButton(
                                label = "×",
                                category = ButtonCategory.Operator,
                                onClick = { viewModel.onOperatorClick("×") },
                                modifier = Modifier.weight(1f),
                                tag = "btn_multiply"
                            )
                        }

                        // Row 3: 4, 5, 6, -
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CalculatorButton(
                                label = "4",
                                category = ButtonCategory.Numeric,
                                onClick = { viewModel.onDigitClick("4") },
                                modifier = Modifier.weight(1f),
                                tag = "btn_4"
                            )
                            CalculatorButton(
                                label = "5",
                                category = ButtonCategory.Numeric,
                                onClick = { viewModel.onDigitClick("5") },
                                modifier = Modifier.weight(1f),
                                tag = "btn_5"
                            )
                            CalculatorButton(
                                label = "6",
                                category = ButtonCategory.Numeric,
                                onClick = { viewModel.onDigitClick("6") },
                                modifier = Modifier.weight(1f),
                                tag = "btn_6"
                            )
                            CalculatorButton(
                                label = "-",
                                category = ButtonCategory.Operator,
                                onClick = { viewModel.onOperatorClick("-") },
                                modifier = Modifier.weight(1f),
                                tag = "btn_minus"
                            )
                        }

                        // Row 4: 1, 2, 3, +
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CalculatorButton(
                                label = "1",
                                category = ButtonCategory.Numeric,
                                onClick = { viewModel.onDigitClick("1") },
                                modifier = Modifier.weight(1f),
                                tag = "btn_1"
                            )
                            CalculatorButton(
                                label = "2",
                                category = ButtonCategory.Numeric,
                                onClick = { viewModel.onDigitClick("2") },
                                modifier = Modifier.weight(1f),
                                tag = "btn_2"
                            )
                            CalculatorButton(
                                label = "3",
                                category = ButtonCategory.Numeric,
                                onClick = { viewModel.onDigitClick("3") },
                                modifier = Modifier.weight(1f),
                                tag = "btn_3"
                            )
                            CalculatorButton(
                                label = "+",
                                category = ButtonCategory.Operator,
                                onClick = { viewModel.onOperatorClick("+") },
                                modifier = Modifier.weight(1f),
                                tag = "btn_plus"
                            )
                        }

                        // Row 5: 0, ., Backspace, =
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CalculatorButton(
                                label = "0",
                                category = ButtonCategory.Numeric,
                                onClick = { viewModel.onDigitClick("0") },
                                modifier = Modifier.weight(1f),
                                tag = "btn_0"
                            )
                            CalculatorButton(
                                label = ".",
                                category = ButtonCategory.Numeric,
                                onClick = { viewModel.onDecimalClick() },
                                modifier = Modifier.weight(1f),
                                tag = "btn_decimal"
                            )
                            CalculatorButton(
                                isIcon = true,
                                label = "Backspace",
                                category = ButtonCategory.Action,
                                onClick = { viewModel.onDeleteClick() },
                                modifier = Modifier.weight(1f),
                                tag = "btn_delete"
                            )
                            CalculatorButton(
                                label = "=",
                                category = ButtonCategory.Equals,
                                onClick = { viewModel.onEqualsClick() },
                                modifier = Modifier.weight(1f),
                                tag = "btn_equals"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdvancedKey(
    label: String,
    onClick: () -> Unit,
    tag: String,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.primary
        ),
        modifier = modifier
            .testTag(tag)
            .size(width = 68.dp, height = 40.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

@Composable
fun HistoryRowItem(
    item: HistoryItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.End
    ) {
        Text(
            text = item.expression,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "= ${item.result}",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

enum class ButtonCategory {
    Numeric, Operator, Action, Equals
}

@Composable
fun CalculatorButton(
    label: String,
    category: ButtonCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isIcon: Boolean = false,
    tag: String = ""
) {
    val haptic = LocalHapticFeedback.current

    val containerColor = when (category) {
        ButtonCategory.Numeric -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ButtonCategory.Operator -> MaterialTheme.colorScheme.secondaryContainer
        ButtonCategory.Action -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.8f)
        ButtonCategory.Equals -> MaterialTheme.colorScheme.primary
    }

    val contentColor = when (category) {
        ButtonCategory.Numeric -> MaterialTheme.colorScheme.onSurfaceVariant
        ButtonCategory.Operator -> MaterialTheme.colorScheme.onSecondaryContainer
        ButtonCategory.Action -> MaterialTheme.colorScheme.onTertiaryContainer
        ButtonCategory.Equals -> MaterialTheme.colorScheme.onPrimary
    }

    Card(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        modifier = modifier
            .testTag(tag)
            .aspectRatio(1f)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (isIcon && label == "Backspace") {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "Backspace",
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = if (category == ButtonCategory.Numeric) FontWeight.Medium else FontWeight.Bold,
                        fontSize = if (label.length > 2) 18.sp else 24.sp
                    )
                )
            }
        }
    }
}
