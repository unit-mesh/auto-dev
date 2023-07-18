package cc.unitmesh.devti.provider

import cc.unitmesh.devti.prompting.model.FinalCodePrompt
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.psi.PsiElement
import com.knuddels.jtokkit.Encodings
import com.knuddels.jtokkit.api.Encoding
import com.knuddels.jtokkit.api.EncodingRegistry
import com.knuddels.jtokkit.api.EncodingType

private var registry: EncodingRegistry? = Encodings.newDefaultEncodingRegistry()
private var encoding: Encoding = registry?.getEncoding(EncodingType.CL100K_BASE)!!

interface PromptStrategy {
    // The default OpenAI Token will be 4096, we leave 2048 for the code.
    fun tokenLength(): Int = 2048
    fun countTokens(code: String): Int = encoding.countTokens(code)
    fun advice(prefixCode: String, suffixCode: String): FinalCodePrompt
    fun advice(psiFile: PsiElement, calleeName: String = ""): FinalCodePrompt
    fun advice(psiFile: PsiElement, usedMethod: List<String>, noExistMethods: List<String>): FinalCodePrompt

    companion object {
        private val EP_NAME: ExtensionPointName<PromptStrategy> =
            ExtensionPointName.create("cc.unitmesh.promptStrategy")

        fun strategy(): PromptStrategy? = EP_NAME.extensionList.firstOrNull()
    }

}
