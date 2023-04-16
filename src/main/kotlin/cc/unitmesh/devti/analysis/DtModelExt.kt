package cc.unitmesh.devti.analysis

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil

fun DtClass.fromPsiClass(psiClass: PsiClass): DtClass {
    val methods = psiClass.methods.map { method ->
        DtMethod(
            name = method.name,
            returnType = method.returnType.toString(),
            parameters = method.parameters.map { parameter ->
                DtParameter(
                    name = parameter.name ?: "",
                    type = parameter.type.toString()
                )
            }
        )
    }
    return DtClass(
        name = psiClass.name ?: "",
        methods = methods
    )
}

fun DtClass.fromPsiFile(psiFile: PsiFile): DtClass? {
    val psiClass = PsiTreeUtil.findChildrenOfType(psiFile, PsiClass::class.java)
        .firstOrNull()

    return psiClass?.let { fromPsiClass(it) }
}

