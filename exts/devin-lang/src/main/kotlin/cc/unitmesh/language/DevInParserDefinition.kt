package cc.unitmesh.language

import cc.unitmesh.language.parser.DevInParser
import cc.unitmesh.language.psi.DevInFile
import cc.unitmesh.language.psi.DevInTypes
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
    override fun createLexer(project: Project?): Lexer {
        return DevInLexerAdapter()
    }

    @NotNull
    override fun getCommentTokens(): TokenSet = TokenSet.EMPTY

    @NotNull
    override fun getStringLiteralElements(): TokenSet = TokenSet.EMPTY

    @NotNull
    override fun createParser(project: Project?): PsiParser {
        return DevInParser()
    }

    @NotNull
    override fun getFileNodeType(): IFileElementType {
        return FILE
    }

    @NotNull
    override fun createFile(@NotNull viewProvider: FileViewProvider): PsiFile {
        return DevInFile(viewProvider)
    }

    @NotNull
    override fun createElement(node: ASTNode?): PsiElement {
//        return DevInTypes.Factory.createElement(node)
        TODO()
    }

    companion object {
        val FILE: IFileElementType = IFileElementType(DevInLanguage.INSTANCE)
    }
}