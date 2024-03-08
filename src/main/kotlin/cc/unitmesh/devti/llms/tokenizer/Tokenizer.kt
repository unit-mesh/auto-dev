package cc.unitmesh.devti.llms.tokenizer

import com.knuddels.jtokkit.api.IntArrayList

interface Tokenizer {
    fun getMaxLength(): Int
    fun count(string: String): Int
    fun tokenize(chunk: String): IntArrayList?
}