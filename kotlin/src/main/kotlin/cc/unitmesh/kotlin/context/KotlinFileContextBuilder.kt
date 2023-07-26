package cc.unitmesh.kotlin.context

import cc.unitmesh.devti.context.FileContext
import cc.unitmesh.devti.context.builder.FileContextBuilder
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.annotations.NotNull
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtImportList
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPackageDirective

class KotlinFileContextBuilder : FileContextBuilder {
    @NotNull
    override fun getFileContext(psiFile: PsiFile): FileContext {
        val name = psiFile.name
        val path = psiFile.virtualFile?.path ?: ""

        val packageDirective = PsiTreeUtil.getChildrenOfTypeAsList(psiFile, KtPackageDirective::class.java).firstOrNull()
        val packageName = packageDirective?.text ?: ""

        val importList = PsiTreeUtil.getChildrenOfTypeAsList(psiFile, KtImportList::class.java)
        val imports = importList.flatMap { it.imports }

        val classOrObjects = PsiTreeUtil.getChildrenOfTypeAsList(psiFile, KtClassOrObject::class.java)
        val namedFunctions = PsiTreeUtil.getChildrenOfTypeAsList(psiFile, KtNamedFunction::class.java)

        return FileContext(psiFile, name, path, packageName, imports, classOrObjects, namedFunctions)
    }
}
