package cc.unitmesh.devti.context

import cc.unitmesh.devti.context.base.LLMCodeContext
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
) : LLMCodeContext {
    private fun getClassDetail(): List<String> = classes.map {
        ClassContextProvider(false).from(it).format()
    }

    override fun format(): String {
        fun getFieldString(fieldName: String, fieldValue: String): String {
            return if (fieldValue.isNotBlank()) "$fieldName: $fieldValue" else ""
        }

        val filePackage = getFieldString("file package", packageString ?: "")
        val fileImports = getFieldString(
            "file imports",
            if (imports.isNotEmpty()) imports.joinToString(" ", transform = { it.text }) else ""
        )
        val classDetails =
            getFieldString(
                "file classes",
                if (getClassDetail().isNotEmpty()) getClassDetail().joinToString(", ") else ""
            )
        val filePath = getFieldString("file path", path)

        return buildString {
            append("file name: $name\n")
            if (filePackage.isNotEmpty()) append("$filePackage\n")
            if (fileImports.isNotEmpty()) append("$fileImports\n")
            if (classDetails.isNotEmpty()) append("$classDetails\n")
            append("$filePath\n")
        }
    }
}
