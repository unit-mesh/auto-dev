package cc.unitmesh.devti.connector.openai

import com.intellij.openapi.util.NlsSafe
import java.io.InputStream

class PromptGenerator {
    fun codeComplete(methodCode: String, className: @NlsSafe String?): String {
        val promptText: InputStream =
            this::class.java.classLoader.getResourceAsStream("prompts/openai/copilot/code_complete.txt")!!
        val promptTextString = promptText.bufferedReader().use { it.readText() }
        return promptTextString
            .replace("{code}", methodCode)
            .replace("{className}", className ?: "")
    }

    fun autoComment(methodCode: String): String {
        val promptText: InputStream =
            this::class.java.classLoader.getResourceAsStream("prompts/openai/copilot/code_comments.txt")!!
        val promptTextString = promptText.bufferedReader().use { it.readText() }
        return promptTextString
            .replace("{code}", methodCode)
    }

    fun codeReview(text: String): String {
        val promptText: InputStream =
            this::class.java.classLoader.getResourceAsStream("prompts/openai/copilot/code_review.txt")!!
        val promptTextString = promptText.bufferedReader().use { it.readText() }
        return promptTextString
            .replace("{code}", text)
    }

    fun findBug(text: String): String {
        val promptText: InputStream =
            this::class.java.classLoader.getResourceAsStream("prompts/openai/copilot/find_bug.txt")!!
        val promptTextString = promptText.bufferedReader().use { it.readText() }
        return promptTextString
            .replace("{code}", text)
    }
}
