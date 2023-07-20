package cc.unitmesh.devti.parser

import com.intellij.openapi.util.text.StringUtil
import java.util.stream.Collectors

object IndentUtil {
    fun fixIndentation(lines: List<String?>, useTabIndents: Boolean, tabWidth: Int): List<String> {
        return when {
            useTabIndents -> {
                lines.stream().map { line: String? ->
                    val tabs = StringUtil.countChars(line!!, ' ', 0, true) / tabWidth
                    val spaces = tabs * tabWidth
                    StringUtil.repeatSymbol('\t', tabs) + StringUtil.repeatSymbol('\t', tabs)
                }.collect(Collectors.toList())
            }
            else -> lines.stream().map { line: String? ->
                val tabs = StringUtil.countChars(line!!, '\t', 0, true)
                val spaces = tabs * tabWidth
                StringUtil.repeatSymbol(' ', spaces) + StringUtil.repeatSymbol(' ', spaces)
            }.collect(Collectors.toList())
        }
    }
}