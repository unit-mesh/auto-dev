package cc.unitmesh.cpp.context

import cc.unitmesh.devti.context.FileContext
import cc.unitmesh.devti.context.builder.FileContextBuilder
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.cidr.lang.psi.OCFile
import com.jetbrains.cidr.lang.psi.OCFunctionDeclaration
import com.jetbrains.cidr.lang.psi.OCIncludeDirective
import com.jetbrains.cidr.lang.psi.OCStructLike


class CppFileContextBuilder : FileContextBuilder {
    override fun getFileContext(psiFile: PsiFile): FileContext? {
        if (psiFile !is OCFile) return null

        val name = psiFile.name
        val path = psiFile.virtualFile.path

        val includes = PsiTreeUtil.getChildrenOfTypeAsList(psiFile, OCIncludeDirective::class.java)
        val structLikes = PsiTreeUtil.getChildrenOfTypeAsList(psiFile, OCStructLike::class.java)
        val functions = PsiTreeUtil.getChildrenOfTypeAsList(psiFile, OCFunctionDeclaration::class.java)

        return FileContext(psiFile, name, path, null, includes, structLikes, functions)
    }
}
