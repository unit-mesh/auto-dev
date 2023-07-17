package cc.unitmesh.devti.context

import cc.unitmesh.devti.context.base.NamedElementContext
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference

class ClassContext(
    override val root: PsiElement,
    override val text: String?,
    override val name: String?,
    val methods: List<PsiElement>,
    val fields: List<PsiElement>,
    val superClasses: List<String>?,
    val usages: List<PsiReference>
) : NamedElementContext(root, text, name) {

}
