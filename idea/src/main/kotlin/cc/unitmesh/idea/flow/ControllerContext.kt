package cc.unitmesh.idea.flow

import com.intellij.psi.PsiClass

data class ControllerContext(
    val services: List<PsiClass>,
    val models: List<PsiClass>,
    val repository: List<PsiClass> = listOf(),
)