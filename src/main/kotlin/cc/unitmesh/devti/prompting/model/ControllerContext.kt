package cc.unitmesh.devti.prompting.model

import com.intellij.psi.PsiClass

data class ControllerContext(
    val services: List<PsiClass>,
    val models: List<PsiClass>,
)