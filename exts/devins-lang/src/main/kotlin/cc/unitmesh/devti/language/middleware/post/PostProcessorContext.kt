package cc.unitmesh.devti.language.middleware.post

import com.intellij.lang.Language
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

class PostProcessorContext(
    /**
     * Convert code to file
     */
    var currentFile: PsiFile? = null,

    /**
     * The language of the code to be handled, which will parse from the GenText when parse code
     */
    var currentLanguage: Language? = null,

    /**
     * Target Language
     */
    var genTargetLanguage: Language? = null,

    var genTargetExtension: String? = null,

    /**
     * The element to be handled, which will be load from current editor when parse code
     */
    var genPsiElement: PsiElement? = null,

    /**
     * The generated text to be handled
     */
    var genText: String? = null,

    /**
     * The data to be passed to the post-processor
     */
    val pipeData: MutableMap<String, Any> = mutableMapOf(),

    /**
     * post text range
     */
    val modifiedTextRange: TextRange? = null,

    /**
     * current editor for modify
     */
    val editor: Editor? = null,

    var lastTaskOutput: String? = null,

    var compiledVariables: Map<String, Any?> = mapOf(),

    val llmModelName: String? = null,
) {
    companion object {
        private val DATA_KEY: Key<PostProcessorContext> = Key.create(PostProcessorContext::class.java.name)
        private val userDataHolderBase = UserDataHolderBase()

        // todo: refactor to GlobalVariableContext
        fun updateContextAndVariables(context: PostProcessorContext) {
            context.compiledVariables = dynamicUpdateVariables(context.compiledVariables)
            userDataHolderBase.putUserData(DATA_KEY, context)
        }

        private fun dynamicUpdateVariables(variables: Map<String, Any?>): MutableMap<String, Any?> {
            val userData = userDataHolderBase.getUserData(DATA_KEY)
            val oldVariables: MutableMap<String, Any?> =
                userData?.compiledVariables?.toMutableMap() ?: mutableMapOf()

            variables.forEach {
                if (it.value.toString().startsWith("$")) {
                    oldVariables.remove(it.key)
                } else if (it.value != null && it.value.toString().isNotEmpty()) {
                    oldVariables[it.key] = it.value
                }
            }

            return oldVariables
        }

        fun getData(): PostProcessorContext? {
            return userDataHolderBase.getUserData(DATA_KEY)
        }

        fun updateOutput(output: Any?) {
            val context = getData()
            if (context != null) {
                context.lastTaskOutput = output.toString()
                updateContextAndVariables(context)
            }

            val compiledVariables = context?.compiledVariables?.toMutableMap()
            compiledVariables?.set("output", output)

            if (context != null) {
                context.compiledVariables = compiledVariables ?: mapOf()
                updateContextAndVariables(context)
            }
        }

        fun updateRunConfigVariables(variables: Map<String, String>) {
            val context = getData()
            val compiledVariables = context?.compiledVariables?.toMutableMap()
            compiledVariables?.putAll(variables)

            if (context != null) {
                context.compiledVariables = compiledVariables ?: mapOf()
                updateContextAndVariables(context)
            }
        }
    }
}