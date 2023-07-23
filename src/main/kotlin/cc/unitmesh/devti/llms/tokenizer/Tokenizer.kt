package cc.unitmesh.devti.llms.tokenizer

interface Tokenizer {
    fun getMaxLength(): Int
    fun count(string: String): Int
}