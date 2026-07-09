package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.HistoryItem
import com.example.data.HistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CalculatorViewModel(private val repository: HistoryRepository) : ViewModel() {

    private val _expression = MutableStateFlow("")
    val expression: StateFlow<String> = _expression.asStateFlow()

    private val _previewResult = MutableStateFlow("")
    val previewResult: StateFlow<String> = _previewResult.asStateFlow()

    private val _isHistoryExpanded = MutableStateFlow(false)
    val isHistoryExpanded: StateFlow<Boolean> = _isHistoryExpanded.asStateFlow()

    val history: StateFlow<List<HistoryItem>> = repository.allHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Observe expression and calculate real-time preview
        viewModelScope.launch {
            _expression.collect { expr ->
                calculatePreview(expr)
            }
        }
    }

    fun onDigitClick(digit: String) {
        val current = _expression.value
        if (current == "0") {
            _expression.value = digit
        } else {
            _expression.value = current + digit
        }
    }

    fun onOperatorClick(op: String) {
        val current = _expression.value
        if (current.isEmpty()) {
            if (op == "-") {
                _expression.value = "-"
            }
            return
        }

        if (current.endsWith(" + ") || current.endsWith(" - ") || current.endsWith(" × ") || current.endsWith(" ÷ ")) {
            _expression.value = current.dropLast(3) + " $op "
        } else if (current.last() == ' ') {
            // fallback
            _expression.value = current.trim() + " $op "
        } else {
            _expression.value = current + " $op "
        }
    }

    fun onDecimalClick() {
        val current = _expression.value
        val lastPart = current.split(" ").last()
        if (!lastPart.contains(".")) {
            if (lastPart.isEmpty() || current.endsWith(" ")) {
                _expression.value = current + "0."
            } else {
                _expression.value = current + "."
            }
        }
    }

    fun onClearClick() {
        _expression.value = ""
        _previewResult.value = ""
    }

    fun onDeleteClick() {
        val current = _expression.value
        if (current.isEmpty()) return

        if (current.endsWith(" + ") || current.endsWith(" - ") || current.endsWith(" × ") || current.endsWith(" ÷ ")) {
            _expression.value = current.dropLast(3)
        } else {
            _expression.value = current.dropLast(1)
        }
    }

    fun onToggleSignClick() {
        val current = _expression.value
        if (current.isEmpty()) {
            _expression.value = "-"
            return
        }
        if (current == "-") {
            _expression.value = ""
            return
        }

        val parts = current.split(" ").toMutableList()
        if (parts.isNotEmpty()) {
            val lastPart = parts.last()
            if (lastPart.isNotEmpty() && lastPart != "+" && lastPart != "-" && lastPart != "×" && lastPart != "÷") {
                if (lastPart.startsWith("-")) {
                    parts[parts.lastIndex] = lastPart.substring(1)
                } else {
                    parts[parts.lastIndex] = "-$lastPart"
                }
                _expression.value = parts.joinToString(" ")
            }
        }
    }

    fun onPercentClick() {
        val current = _expression.value
        if (current.isNotEmpty() && (current.last().isDigit() || current.last() == ')')) {
            _expression.value = current + "%"
        }
    }

    fun onParenthesisClick(parenthesis: String) {
        val current = _expression.value
        if (current == "0" && parenthesis == "(") {
            _expression.value = "("
        } else {
            _expression.value = current + parenthesis
        }
    }

    fun onEqualsClick() {
        val current = _expression.value
        if (current.isEmpty()) return

        try {
            val cleaned = cleanExpressionForEvaluation(current)
            if (cleaned.isEmpty()) return

            val parser = ExpressionParser(cleaned)
            val resultVal = parser.parse()
            val formatted = formatResult(resultVal)

            if (formatted != "Error") {
                viewModelScope.launch {
                    repository.insert(HistoryItem(expression = current, result = formatted))
                }
            }

            _expression.value = formatted
            _previewResult.value = ""
        } catch (e: Exception) {
            _previewResult.value = "Error"
        }
    }

    fun onHistoryItemClick(item: HistoryItem) {
        _expression.value = item.expression
        _isHistoryExpanded.value = false
    }

    fun onClearHistoryClick() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    fun onToggleHistoryExpanded() {
        _isHistoryExpanded.value = !_isHistoryExpanded.value
    }

    private fun calculatePreview(expr: String) {
        val hasOperator = expr.any { it in listOf('+', '-', '×', '÷') } || expr.contains("%") || expr.contains("(")
        if (!hasOperator || expr.trim() == "-") {
            _previewResult.value = ""
            return
        }

        try {
            val cleaned = cleanExpressionForEvaluation(expr)
            if (cleaned.isEmpty()) {
                _previewResult.value = ""
                return
            }

            val parser = ExpressionParser(cleaned)
            val resultVal = parser.parse()
            val formatted = formatResult(resultVal)
            if (formatted != expr.trim()) {
                _previewResult.value = "= $formatted"
            } else {
                _previewResult.value = ""
            }
        } catch (e: Exception) {
            _previewResult.value = ""
        }
    }

    private fun cleanExpressionForEvaluation(expr: String): String {
        var temp = expr.trim()
        if (temp.isEmpty()) return ""

        // Normalize visual operators
        temp = temp.replace("×", "*").replace("÷", "/")

        // Strip trailing operators iteratively
        while (temp.endsWith("+") || temp.endsWith("-") || temp.endsWith("*") || temp.endsWith("/")) {
            temp = temp.dropLast(1).trim()
        }

        // Auto-close open parentheses if needed for evaluation
        val openCount = temp.count { it == '(' }
        val closeCount = temp.count { it == ')' }
        if (openCount > closeCount) {
            temp += ")".repeat(openCount - closeCount)
        }

        return temp
    }

    private fun formatResult(value: Double): String {
        if (value.isInfinite() || value.isNaN()) return "Error"
        if (value == value.toLong().toDouble()) {
            return value.toLong().toString()
        }
        val df = java.text.DecimalFormat("#.##########")
        df.roundingMode = java.math.RoundingMode.HALF_UP
        return df.format(value)
    }
}

class ExpressionParser(private val str: String) {
    private var pos = -1
    private var ch = 0

    private fun nextChar() {
        ch = if (++pos < str.length) str[pos].code else -1
    }

    private fun eat(charToEat: Int): Boolean {
        while (ch == ' '.code) nextChar()
        if (ch == charToEat) {
            nextChar()
            return true
        }
        return false
    }

    fun parse(): Double {
        if (str.isBlank()) return 0.0
        nextChar()
        val x = parseExpression()
        if (pos < str.length) throw RuntimeException("Unexpected: " + ch.toChar())
        return x
    }

    private fun parseExpression(): Double {
        var x = parseTerm()
        while (true) {
            if (eat('+'.code)) {
                x += parseTerm()
            } else if (eat('-'.code)) {
                x -= parseTerm()
            } else {
                break
            }
        }
        return x
    }

    private fun parseTerm(): Double {
        var x = parseFactor()
        while (true) {
            if (eat('*'.code)) {
                x *= parseFactor()
            } else if (eat('/'.code)) {
                val divisor = parseFactor()
                if (divisor == 0.0) throw ArithmeticException("Divide by zero")
                x /= divisor
            } else {
                break
            }
        }
        return x
    }

    private fun parseFactor(): Double {
        if (eat('+'.code)) return parseFactor()
        if (eat('-'.code)) return -parseFactor()

        var x: Double
        val startPos = this.pos
        if (eat('('.code)) {
            x = parseExpression()
            eat(')'.code)
        } else if ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) {
            while ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) {
                nextChar()
            }
            val numStr = str.substring(startPos, this.pos)
            x = numStr.toDoubleOrNull() ?: throw RuntimeException("Invalid number: $numStr")
        } else {
            throw RuntimeException("Unexpected character: " + ch.toChar())
        }

        if (eat('%'.code)) {
            x /= 100.0
        }

        return x
    }
}
