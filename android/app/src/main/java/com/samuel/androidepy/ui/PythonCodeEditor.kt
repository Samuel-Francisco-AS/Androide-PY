package com.samuel.androidepy.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PythonCodeEditor(
    sourceCode: String,
    onSourceCodeChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colors = MaterialTheme.colorScheme
    val lineCount = sourceCode.count { it == '\n' } + 1
    val editorStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = colors.onSurface
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 360.dp)
            .border(1.dp, colors.outlineVariant, RoundedCornerShape(12.dp))
            .padding(10.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = (1..lineCount).joinToString("\n"),
            style = editorStyle.copy(color = colors.onSurfaceVariant),
            modifier = Modifier.padding(end = 12.dp)
        )
        Box(modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
            BasicTextField(
                value = sourceCode,
                onValueChange = onSourceCodeChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                textStyle = editorStyle,
                cursorBrush = SolidColor(colors.primary),
                minLines = maxOf(12, lineCount),
                maxLines = maxOf(12, lineCount),
                visualTransformation = PythonSyntaxTransformation(
                    keywordColor = colors.primary,
                    stringColor = colors.tertiary,
                    commentColor = colors.onSurfaceVariant,
                    numberColor = colors.secondary
                )
            )
        }
    }
}

private class PythonSyntaxTransformation(
    private val keywordColor: Color,
    private val stringColor: Color,
    private val commentColor: Color,
    private val numberColor: Color
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val builder = AnnotatedString.Builder(text.text)
        val source = text.text
        var index = 0
        while (index < source.length) {
            val start = index
            val current = source[index]
            when {
                current == '#' -> {
                    while (index < source.length && source[index] != '\n') index++
                    builder.addStyle(SpanStyle(color = commentColor), start, index)
                }
                current == '\'' || current == '"' -> {
                    val quote = current
                    index++
                    while (index < source.length) {
                        if (source[index] == '\\') {
                            index = (index + 2).coerceAtMost(source.length)
                        } else if (source[index] == quote) {
                            index++
                            break
                        } else {
                            index++
                        }
                    }
                    builder.addStyle(SpanStyle(color = stringColor), start, index)
                }
                current.isLetter() || current == '_' -> {
                    index++
                    while (index < source.length && (source[index].isLetterOrDigit() || source[index] == '_')) index++
                    if (source.substring(start, index) in PYTHON_KEYWORDS) {
                        builder.addStyle(SpanStyle(color = keywordColor), start, index)
                    }
                }
                current.isDigit() -> {
                    index++
                    while (index < source.length && (source[index].isDigit() || source[index] == '.')) index++
                    builder.addStyle(SpanStyle(color = numberColor), start, index)
                }
                else -> index++
            }
        }
        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }

    private companion object {
        val PYTHON_KEYWORDS = setOf(
            "and", "as", "assert", "async", "await", "break", "class", "continue",
            "def", "del", "elif", "else", "except", "False", "finally", "for",
            "from", "global", "if", "import", "in", "is", "lambda", "None",
            "nonlocal", "not", "or", "pass", "raise", "return", "True", "try",
            "while", "with", "yield"
        )
    }
}
