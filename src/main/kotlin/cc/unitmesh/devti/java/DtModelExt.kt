package cc.unitmesh.devti.java

import cc.unitmesh.devti.context.DtClass
import com.intellij.openapi.application.runReadAction
import com.intellij.psi.impl.source.PsiJavaFileImpl

fun fromJavaFile(file: PsiJavaFileImpl?): DtClass {
    return runReadAction {
        val psiClass = file?.classes?.firstOrNull() ?: return@runReadAction DtClass("", emptyList())
        return@runReadAction DtClass.fromPsi(psiClass)
    }
}