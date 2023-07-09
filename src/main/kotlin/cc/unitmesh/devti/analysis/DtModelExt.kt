package cc.unitmesh.devti.analysis

import com.intellij.openapi.application.runReadAction
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil

fun DtClass.Companion.fromPsiClass(psiClass: PsiClass): DtClass {
    val methods = psiClass.methods.map { method ->
        DtMethod(
            name = method.name,
            returnType = method.returnType?.presentableText ?: "",
            parameters = method.parameters.map { parameter ->
                DtParameter(
                    name = parameter.name ?: "",
                    type = parameter.type.toString().replace(" PsiType:", "")
                )
            }
        )
    }
    return DtClass(
        name = psiClass.name ?: "",
        methods = methods
    )
}

fun DtClass.Companion.fromPsiFile(psiFile: PsiFile): DtClass? {
    return runReadAction {
        val psiClass = PsiTreeUtil.findChildrenOfType(psiFile, PsiClass::class.java)
            .firstOrNull()

        return@runReadAction psiClass?.let { fromPsiClass(it) }
    }
}

