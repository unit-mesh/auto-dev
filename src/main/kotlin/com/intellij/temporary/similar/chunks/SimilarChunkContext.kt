// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.temporary.similar.chunks

import cc.unitmesh.devti.context.base.LLMCodeContext
import com.intellij.lang.Language
import com.intellij.lang.LanguageCommenters


class SimilarChunkContext(val language: Language, val paths: List<String>?, val chunks: List<String>?) :
    LLMCodeContext {
    override fun format(): String {
        val commentPrefix = commentPrefix(language) ?: return ""

        if (paths == null || chunks == null) return ""

        val filteredPairs = paths.zip(chunks).filter { it.second.isNotEmpty() }

        val queryBuilder = StringBuilder()
        for ((path, chunk) in filteredPairs) {
            val commentedCode = commentCode(chunk, commentPrefix)
            queryBuilder.append("$commentPrefix Compare this snippet from $path:\n")
            queryBuilder.append(commentedCode).append("\n")
        }

        return queryBuilder.toString().trim()
    }

    private fun commentCode(code: String, commentSymbol: String?): String {
        if (commentSymbol == null) return code

        return code.split("\n").joinToString("\n") {
            "$commentSymbol $it"
        }
    }

    companion object {
        fun commentPrefix(language: Language): String? {
            val commenter = LanguageCommenters.INSTANCE.forLanguage(language)
            val commentPrefix = commenter.lineCommentPrefix
            return commentPrefix
        }
    }
}
