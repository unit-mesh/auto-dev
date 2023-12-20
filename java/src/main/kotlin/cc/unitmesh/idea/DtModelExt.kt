package cc.unitmesh.idea

import cc.unitmesh.devti.context.model.DtClass
import cc.unitmesh.devti.context.model.DtField
import cc.unitmesh.devti.context.model.DtMethod
import cc.unitmesh.devti.context.model.DtParameter
import com.intellij.openapi.application.runReadAction
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.PsiJavaFileImpl

fun fromJavaFile(file: PsiJavaFileImpl?): DtClass {
    return runReadAction {
        val psiClass = file?.classes?.firstOrNull() ?: return@runReadAction DtClass("", emptyList())
        return@runReadAction DtClass.fromPsi(psiClass)
    }
}

private val classCache = mutableMapOf<String, DtClass>()


fun DtClass.Companion.formatPsi(psiClass: PsiClass): String {
    return fromPsi(psiClass).commentFormat()
}

fun DtClass.Companion.fromJavaFile(file: PsiFile): DtClass {
    return fromJavaFile(file as? PsiJavaFileImpl)
}

fun DtClass.Companion.fromPsi(originClass: PsiClass): DtClass {
    classCache[originClass.qualifiedName ?: ""]?.let {
        return it
    }

    val psiClass = runReadAction { originClass.copy() as PsiClass }

    val fields = psiClass.fields.map { field ->
        DtField(
            name = field.name,
            type = field.type.toString().replace("PsiType:", "")
        )
    }

    val methods = psiClass.methods.map { method ->
        // if method is getter or setter, skip
        if (method.name.startsWith("get") || method.name.startsWith("set")) {
            return@map null
        }

        DtMethod(
            name = method.name,
            returnType = method.returnType?.presentableText ?: "",
            parameters = method.parameters.map { parameter ->
                DtParameter(
                    name = parameter.name ?: "",
                    type = parameter.type.toString().replace("PsiType:", "")
                )
            }
        )
    }.filterNotNull()

    val dtClass = DtClass(
        packageName = psiClass.qualifiedName ?: "",
        path = psiClass.containingFile?.virtualFile?.path ?: "",
        name = psiClass.name ?: "",
        methods = methods,
        fields = fields
    )

    classCache[psiClass.qualifiedName ?: ""] = dtClass
    return dtClass
}