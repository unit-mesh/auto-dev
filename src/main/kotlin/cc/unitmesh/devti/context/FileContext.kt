package cc.unitmesh.devti.context

import cc.unitmesh.devti.context.base.LLMQueryContext
import com.google.gson.Gson
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

class FileContext(
    val root: PsiFile,
    val name: String,
    val path: String,
    val packageString: String? = null,
    val imports: List<PsiElement> = emptyList(),
    val classes: List<PsiElement> = emptyList(),
    val methods: List<PsiElement> = emptyList(),
) : LLMQueryContext {
    fun getClassNames(): List<String> = classes.mapNotNull {
        ClassContextProvider(false).from(it).name
    }

    override fun toQuery(): String {
        val filePackage = "_"
        val fileImports = imports.joinToString(" ", transform = { it.text })
        val fileClassNames = getClassNames().joinToString(", ")
        return "file name: $name\nfile path: $path\nfile package: $filePackage\nfile imports: $fileImports\nfile classes: $fileClassNames"
    }

    override fun toJson(): String {
        return Gson().toJson(
            mapOf(
                "name" to name,
                "path" to path,
                "package" to packageString
            )
        )
    }
}
