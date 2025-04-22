package cc.unitmesh.devti.language.highlight

import cc.unitmesh.devti.language.lexer.DevInLexerAdapter
import cc.unitmesh.devti.language.psi.DevInTypes
import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet

class DevInSyntaxHighlighter : SyntaxHighlighterBase() {
    override fun getHighlightingLexer(): Lexer = DevInLexerAdapter()

    override fun getTokenHighlights(tokenType: IElementType?): Array<TextAttributesKey> {
        return pack(ATTRIBUTES[tokenType])
    }

    companion object {
        private val ATTRIBUTES: MutableMap<IElementType, TextAttributesKey> = HashMap()

        private val KEYWORDS: TokenSet = TokenSet.create(
            DevInTypes.CASE,
            DevInTypes.DEFAULT,
            DevInTypes.SELECT,
            DevInTypes.WHERE,
            DevInTypes.FROM,
            DevInTypes.IF,
            DevInTypes.ELSE,
            DevInTypes.ELSEIF,
            DevInTypes.END,
            DevInTypes.ENDIF,
            DevInTypes.AND,

            // true and false
            DevInTypes.BOOLEAN,

            // lifecycle
            DevInTypes.WHEN,
            DevInTypes.BEFORE_STREAMING,
            DevInTypes.ON_STREAMING,
            DevInTypes.ON_STREAMING_END,
            DevInTypes.AFTER_STREAMING,
        )

        init {
            fillMap(
                ATTRIBUTES,
                KEYWORDS,
                DefaultLanguageHighlighterColors.KEYWORD
            )

            ATTRIBUTES[DevInTypes.COMMENTS] = DefaultLanguageHighlighterColors.LINE_COMMENT
            ATTRIBUTES[DevInTypes.CONTENT_COMMENTS] = DefaultLanguageHighlighterColors.LINE_COMMENT
            ATTRIBUTES[DevInTypes.BLOCK_COMMENT] = DefaultLanguageHighlighterColors.BLOCK_COMMENT

            ATTRIBUTES[DevInTypes.VARIABLE_START] = DefaultLanguageHighlighterColors.KEYWORD
            ATTRIBUTES[DevInTypes.VARIABLE_ID] = DefaultLanguageHighlighterColors.CONSTANT

            ATTRIBUTES[DevInTypes.FOREIGN_TYPE] = DefaultLanguageHighlighterColors.KEYWORD
            ATTRIBUTES[DevInTypes.OUTPUT_VAR] = DefaultLanguageHighlighterColors.LOCAL_VARIABLE
            ATTRIBUTES[DevInTypes.ACCESS] = DefaultLanguageHighlighterColors.KEYWORD
            ATTRIBUTES[DevInTypes.PROCESS] = DefaultLanguageHighlighterColors.KEYWORD

            ATTRIBUTES[DevInTypes.AGENT_START] = DefaultLanguageHighlighterColors.KEYWORD
            ATTRIBUTES[DevInTypes.AGENT_ID] = DefaultLanguageHighlighterColors.CONSTANT

            ATTRIBUTES[DevInTypes.COMMAND_START] = DefaultLanguageHighlighterColors.KEYWORD
            ATTRIBUTES[DevInTypes.COMMAND_ID] = DefaultLanguageHighlighterColors.KEYWORD
            ATTRIBUTES[DevInTypes.COMMAND_PROP] = DefaultLanguageHighlighterColors.STRING

            ATTRIBUTES[DevInTypes.SHARP] = DefaultLanguageHighlighterColors.CONSTANT
            ATTRIBUTES[DevInTypes.MARKDOWN_HEADER] = DefaultLanguageHighlighterColors.CONSTANT

            ATTRIBUTES[DevInTypes.LINE_INFO] = DefaultLanguageHighlighterColors.NUMBER

            ATTRIBUTES[DevInTypes.CODE_BLOCK_START] = DefaultLanguageHighlighterColors.KEYWORD
            ATTRIBUTES[DevInTypes.CODE_BLOCK_END] = DefaultLanguageHighlighterColors.KEYWORD
            ATTRIBUTES[DevInTypes.LANGUAGE_ID] = DefaultLanguageHighlighterColors.CONSTANT

            ATTRIBUTES[DevInTypes.NUMBER] = DefaultLanguageHighlighterColors.NUMBER

            ATTRIBUTES[DevInTypes.FRONTMATTER_START] = DefaultLanguageHighlighterColors.KEYWORD
            ATTRIBUTES[DevInTypes.FRONTMATTER_END] = DefaultLanguageHighlighterColors.KEYWORD

            ATTRIBUTES[DevInTypes.FRONT_MATTER_ID] = DefaultLanguageHighlighterColors.CONSTANT

            // func name
            ATTRIBUTES[DevInTypes.IDENTIFIER] = DefaultLanguageHighlighterColors.IDENTIFIER
            ATTRIBUTES[DevInTypes.NUMBER] = DefaultLanguageHighlighterColors.KEYWORD
            ATTRIBUTES[DevInTypes.QUOTE_STRING] = DefaultLanguageHighlighterColors.STRING
            ATTRIBUTES[DevInTypes.DATE] = DefaultLanguageHighlighterColors.LABEL

            ATTRIBUTES[DevInTypes.LBRACKET] = DefaultLanguageHighlighterColors.BRACKETS
            ATTRIBUTES[DevInTypes.RBRACKET] = DefaultLanguageHighlighterColors.BRACKETS

            ATTRIBUTES[DevInTypes.OPEN_BRACE] = DefaultLanguageHighlighterColors.BRACES
            ATTRIBUTES[DevInTypes.CLOSE_BRACE] = DefaultLanguageHighlighterColors.BRACES

            ATTRIBUTES[DevInTypes.LPAREN] = DefaultLanguageHighlighterColors.PARENTHESES
            ATTRIBUTES[DevInTypes.RPAREN] = DefaultLanguageHighlighterColors.PARENTHESES
        }
    }

}
