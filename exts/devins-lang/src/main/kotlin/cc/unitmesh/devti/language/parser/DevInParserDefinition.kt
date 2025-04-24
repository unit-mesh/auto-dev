package cc.unitmesh.devti.language.parser

import cc.unitmesh.devti.language.DevInLanguage
import cc.unitmesh.devti.language.lexer.DevInLexerAdapter
import cc.unitmesh.devti.language.psi.DevInFile
import cc.unitmesh.devti.language.psi.DevInTypes
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import org.jetbrains.annotations.NotNull


internal class DevInParserDefinition : ParserDefinition {
    @NotNull
    override fun createLexer(project: Project?): Lexer = DevInLexerAdapter()

    override fun getCommentTokens(): TokenSet = ShireTokenTypeSets.SHIRE_COMMENTS

    @NotNull
    override fun getStringLiteralElements(): TokenSet = TokenSet.EMPTY

    override fun getWhitespaceTokens(): TokenSet = ShireTokenTypeSets.WHITESPACES

    @NotNull
    override fun createParser(project: Project?): PsiParser = DevInParser()

    @NotNull
    override fun getFileNodeType(): IFileElementType = FILE

    @NotNull
    override fun createFile(@NotNull viewProvider: FileViewProvider): PsiFile = DevInFile(viewProvider)

    @NotNull
    override fun createElement(node: ASTNode?): PsiElement {
        return when (node!!.elementType) {
            DevInTypes.CODE -> {
                CodeBlockElement(node)
            }

            DevInTypes.PATTERN -> {
                PatternElement(node)
            }
            DevInTypes.FUNC_CALL -> {
                when (node.firstChildNode.text) {
                    "grep" -> {
                        ShireGrepFuncCall(node)
                    }
                    "sed" -> {
                        ShireSedFuncCall(node)
                    }
                    else -> {
                        DevInTypes.Factory.createElement(node)
                    }
                }
            }
            DevInTypes.CODE_CONTENTS -> {
                ASTWrapperPsiElement(node)
            }

            else -> DevInTypes.Factory.createElement(node)
        }
    }

    companion object {
        val FILE: IFileElementType = IFileElementType(DevInLanguage.INSTANCE)
    }
}