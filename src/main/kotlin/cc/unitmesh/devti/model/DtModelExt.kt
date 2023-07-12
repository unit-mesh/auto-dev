package cc.unitmesh.devti.analysis

import com.intellij.openapi.application.runReadAction
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil

fun DtClass.Companion.fromPsiFile(psiFile: PsiFile): DtClass? {
    return runReadAction {
        val psiClass = PsiTreeUtil.findChildrenOfType(psiFile, PsiClass::class.java)
            .firstOrNull()

        return@runReadAction psiClass?.let { fromPsi(it) }
    }
}

