package cc.unitmesh.devti.language.parser

import cc.unitmesh.devti.language.psi.DevInTypes
import com.intellij.psi.TokenType
import com.intellij.psi.tree.TokenSet

object ShireTokenTypeSets {
    // DevInTypes.NEWLINE
    val WHITESPACES: TokenSet = TokenSet.create(TokenType.WHITE_SPACE)

    val SHIRE_COMMENTS = TokenSet.create(DevInTypes.CONTENT_COMMENTS, DevInTypes.COMMENTS, DevInTypes.BLOCK_COMMENT)
}
