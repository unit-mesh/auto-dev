package cc.unitmesh.ide.javascript.provider

import cc.unitmesh.devti.provider.RelatedClassesProvider
import cc.unitmesh.ide.javascript.util.JSTypeResolver
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

class JavaScriptRelatedClassProvider : RelatedClassesProvider {
    override fun lookupIO(element: PsiElement): List<PsiElement> = JSTypeResolver.resolveByElement(element)
    override fun lookupIO(element: PsiFile): List<PsiElement> = JSTypeResolver.resolveByElement(element)
}
