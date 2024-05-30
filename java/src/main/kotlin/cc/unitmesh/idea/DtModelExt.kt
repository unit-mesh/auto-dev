package cc.unitmesh.idea

import cc.unitmesh.devti.context.model.DtClass
import cc.unitmesh.devti.context.model.DtField
import cc.unitmesh.devti.context.model.DtMethod
import cc.unitmesh.devti.context.model.DtParameter
import com.intellij.openapi.application.runReadAction
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.impl.source.PsiJavaFileImpl

private val fileCache = mutableMapOf<String, DtClass>()

fun fromJavaFile(file: PsiJavaFile?): DtClass {
    val path = file?.virtualFile?.path ?: ""
    val cachedClass = fileCache[path]
    if (cachedClass != null) {
        return cachedClass
    }

    return runReadAction {
        val packageName = file?.packageName ?: ""
        val psiClass = file?.classes?.firstOrNull() ?: return@runReadAction DtClass("", emptyList())
        val fromPsi = DtClass.fromPsi(psiClass, packageName)
        fileCache[path] = fromPsi

        return@runReadAction fromPsi
    }
}

fun DtClass.Companion.formatPsi(psiClass: PsiClass, packageName: String = ""): String {
    return fromPsi(psiClass, packageName).commentFormat()
}

fun DtClass.Companion.fromJavaFile(file: PsiFile): DtClass {
    return fromJavaFile(file as? PsiJavaFileImpl)
}

fun DtClass.Companion.fromPsi(originClass: PsiClass, packageName: String): DtClass {
    val path = originClass.containingFile?.virtualFile?.path ?: ""
    val psiClass = runReadAction { originClass.copy() as PsiClass }

    val fields = psiClass.fields.map { field ->
        DtField(
            name = field.name,
            type = field.type.toString().replace("PsiType:", "")
        )
    }

    val methods = runReadAction {
        psiClass.methods.map { method ->
            // if method is getter or setter, skip
            val parameters = method.parameters
            val methodName = method.name

            val isGetter = methodName.startsWith("get")
                    && parameters.isEmpty()
                    && !(methodName.contains("By") || methodName.contains("With") || methodName.contains("And"))

            val isSetter = methodName.startsWith("set") && parameters.size == 1
            if (isGetter || isSetter) {
                return@map null
            }

            DtMethod(
                name = methodName,
                returnType = method.returnType?.presentableText ?: "",
                parameters = parameters.map { parameter ->
                    DtParameter(
                        name = parameter.name ?: "",
                        type = parameter.type.toString().replace("PsiType:", "")
                    )
                }
            )
        }.filterNotNull()
    }

    val dtClass = DtClass(
        packageName = packageName,
        path = path,
        name = psiClass.name ?: "",
        methods = methods,
        fields = fields
    )

    return dtClass
}