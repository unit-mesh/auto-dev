package cc.unitmesh.devti.language.highlight

import cc.unitmesh.devti.language.lexer.DevInLexerAdapter
import cc.unitmesh.devti.language.psi.DevInTypes
import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType

class DevInSyntaxHighlighter : SyntaxHighlighter {
    override fun getHighlightingLexer(): Lexer = DevInLexerAdapter()

    override fun getTokenHighlights(tokenType: IElementType?): Array<TextAttributesKey> {
        return SyntaxHighlighterBase.pack(ATTRIBUTES[tokenType])
    }

    companion object {
        private val ATTRIBUTES: MutableMap<IElementType, TextAttributesKey> = HashMap()

        init {
            ATTRIBUTES[DevInTypes.COMMENTS] = DefaultLanguageHighlighterColors.LINE_COMMENT

            ATTRIBUTES[DevInTypes.VARIABLE_START] = DefaultLanguageHighlighterColors.KEYWORD
            ATTRIBUTES[DevInTypes.VARIABLE_ID] = DefaultLanguageHighlighterColors.CONSTANT

            ATTRIBUTES[DevInTypes.AGENT_START] = DefaultLanguageHighlighterColors.KEYWORD
            ATTRIBUTES[DevInTypes.AGENT_ID] = DefaultLanguageHighlighterColors.CONSTANT

            ATTRIBUTES[DevInTypes.COMMAND_START] = DefaultLanguageHighlighterColors.KEYWORD
            ATTRIBUTES[DevInTypes.COMMAND_ID] = DefaultLanguageHighlighterColors.KEYWORD
            ATTRIBUTES[DevInTypes.COMMAND_PROP] = DefaultLanguageHighlighterColors.STRING

            ATTRIBUTES[DevInTypes.SHARP] = DefaultLanguageHighlighterColors.CONSTANT
            ATTRIBUTES[DevInTypes.LINE_INFO] = DefaultLanguageHighlighterColors.NUMBER

            ATTRIBUTES[DevInTypes.CODE_BLOCK_START] = DefaultLanguageHighlighterColors.KEYWORD
            ATTRIBUTES[DevInTypes.CODE_BLOCK_END] = DefaultLanguageHighlighterColors.KEYWORD
            ATTRIBUTES[DevInTypes.LANGUAGE_ID] = DefaultLanguageHighlighterColors.CONSTANT

            ATTRIBUTES[DevInTypes.SYSTEM_START] = DefaultLanguageHighlighterColors.KEYWORD
            ATTRIBUTES[DevInTypes.SYSTEM_ID] = DefaultLanguageHighlighterColors.CONSTANT
            ATTRIBUTES[DevInTypes.NUMBER] = DefaultLanguageHighlighterColors.NUMBER
        }
    }

}
