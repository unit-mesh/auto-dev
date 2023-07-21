package cc.unitmesh.devti.llms

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

interface LLMProvider {
    fun prompt(promptText: String): String

    @OptIn(ExperimentalCoroutinesApi::class)
    fun stream(promptText: String): Flow<String> {
        return callbackFlow {
            val prompt = prompt(promptText)
            trySend(prompt)
        }
    }
}

interface CodeCopilotProvider : LLMProvider {
    fun autoComment(text: String): String
    fun findBug(text: String): String
}