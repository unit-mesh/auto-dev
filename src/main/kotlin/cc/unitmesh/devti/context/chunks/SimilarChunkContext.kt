package cc.unitmesh.devti.context.chunks

import cc.unitmesh.devti.context.base.LLMQueryContext
import com.google.gson.Gson
import com.intellij.lang.Language
import com.intellij.lang.LanguageCommenters


class SimilarChunkContext(val language: Language, val paths: List<String>?, val chunks: List<String>?) :
    LLMQueryContext {
    override fun toQuery(): String {
        val commenter = LanguageCommenters.INSTANCE.forLanguage(language)
        val commentPrefix = commenter?.lineCommentPrefix

        if (paths == null || chunks == null) return ""

        val filteredPairs = paths.zip(chunks)
            .filter { it.second.isNotEmpty() }

        val queryBuilder = StringBuilder()
        for ((path, chunk) in filteredPairs) {
            val commentedCode = commentCode(chunk, commentPrefix)
            queryBuilder.append("$commentPrefix Compare this snippet from $path:\n")
            queryBuilder.append(commentedCode).append("\n")
        }

        return queryBuilder.toString().trim()
    }

    override fun toJson(): String = Gson().toJson(
        mapOf(
            "paths" to paths,
            "chunks" to chunks
        )
    )

    private fun commentCode(code: String, commentSymbol: String?): String {
        if (commentSymbol == null) return code

        return code.split("\n").joinToString("\n") {
            "$commentSymbol $it"
        }
    }
}
