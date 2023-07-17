package cc.unitmesh.devti.context

import cc.unitmesh.devti.context.base.LLMQueryContext
import com.google.gson.Gson
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

class FileContext(
    val root: PsiFile,
    val name: String,
    val path: String,
    val packageString: String?,
    val imports: List<PsiElement>,
    val classes: List<PsiElement>,
    val methods: List<PsiElement>
) : LLMQueryContext {

    override fun toQuery(): String {
        return ""
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