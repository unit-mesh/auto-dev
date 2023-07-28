package cc.unitmesh.idea.context

import cc.unitmesh.devti.context.FileContext
import cc.unitmesh.devti.context.builder.FileContextBuilder
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil.getChildrenOfTypeAsList


class JavaFileContextBuilder : FileContextBuilder {

    override fun getFileContext(psiFile: PsiFile): FileContext {
        val packageStatement = getChildrenOfTypeAsList(psiFile, PsiPackageStatement::class.java).firstOrNull()
        val importLists = getChildrenOfTypeAsList(psiFile, PsiImportList::class.java)
        val classDeclarations = getChildrenOfTypeAsList(psiFile, PsiClass::class.java)

        val imports = mutableListOf<PsiElement>()
        for (it in importLists) imports.addAll(it.allImportStatements)

        val packageString = packageStatement?.text
        val path = psiFile.virtualFile.path

        return FileContext(psiFile, psiFile.name, path, packageString, imports, classDeclarations, emptyList())
    }
}
