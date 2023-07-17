package cc.unitmesh.devti.models

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

interface LLMProvider {
    fun prompt(promptText: String): String
    fun stream(promptText: String): Flow<String> {
        return callbackFlow {
            val prompt = prompt(promptText)
            trySend(prompt)
        }
    }
}