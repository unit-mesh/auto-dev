package cc.unitmesh.devti.java

import com.intellij.psi.PsiClass

data class ControllerContext(
    val services: List<PsiClass>,
    val models: List<PsiClass>,
)