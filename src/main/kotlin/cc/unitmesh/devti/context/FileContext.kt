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
    private fun getClassNames(): List<String> = classes.mapNotNull {
        ClassContextProvider(false).from(it).name
    }

    override fun toQuery(): String {
        fun getFieldString(fieldName: String, fieldValue: String): String {
            return if (fieldValue.isNotBlank()) {
                "$fieldName: $fieldValue"
            } else {
                ""
            }
        }

        val filePackage = getFieldString("file package", packageString ?: "")
        val fileImports = getFieldString(
            "file imports",
            if (imports.isNotEmpty()) imports.joinToString(" ", transform = { it.text }) else ""
        )
        val fileClassNames =
            getFieldString("file classes", if (getClassNames().isNotEmpty()) getClassNames().joinToString(", ") else "")
        val filePath = getFieldString("file path", path)

        return buildString {
            append("file name: $name\n")
            if (filePackage.isNotBlank()) {
                append("$filePackage\n")
            }
            if (fileImports.isNotBlank()) {
                append("$fileImports\n")
            }
            if (fileClassNames.isNotBlank()) {
                append("$fileClassNames\n")
            }
            append("$filePath\n")
        }
    }

    override fun toUML(): String {
        return toQuery()
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
