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

    @NotNull
    override fun getCommentTokens(): TokenSet = TokenSet.EMPTY

    @NotNull
    override fun getStringLiteralElements(): TokenSet = TokenSet.EMPTY

    @NotNull
    override fun createParser(project: Project?): PsiParser = DevInParser()

    @NotNull
    override fun getFileNodeType(): IFileElementType = FILE

    @NotNull
    override fun createFile(@NotNull viewProvider: FileViewProvider): PsiFile = DevInFile(viewProvider)

    @NotNull
    override fun createElement(node: ASTNode?): PsiElement {
        val elementType = node!!.elementType
        if (elementType == DevInTypes.CODE) {
            return CodeBlockElement(node)
        }

        if (elementType == DevInTypes.CODE_CONTENTS) {
            return ASTWrapperPsiElement(node)
        }

        return DevInTypes.Factory.createElement(node)
    }

    companion object {
        val FILE: IFileElementType = IFileElementType(DevInLanguage.INSTANCE)
    }
}