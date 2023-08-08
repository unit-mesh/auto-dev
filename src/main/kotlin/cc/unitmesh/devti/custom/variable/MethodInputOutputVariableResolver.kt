package cc.unitmesh.devti.custom.variable

import cc.unitmesh.devti.context.MethodContextProvider
import com.intellij.psi.PsiElement

class MethodInputOutputVariableResolver(val element: PsiElement) : VariableResolver {
    override val type: CustomIntentionVariableType = CustomIntentionVariableType.METHOD_INPUT_OUTPUT

    override fun resolve(): String {
        var result = ""
        val methodContext = MethodContextProvider(false, false).from(element)
        if (methodContext.name == null) {
            return ""
        }

        val input = methodContext.signature ?: ""
        val output = methodContext.returnType ?: ""

        // skip input if it is empty, skip output if it is void
        if (input.isNotEmpty()) {
            result += "Input: $input\n"
        }
        if (output.isNotEmpty() && output != "void") {
            result += "Output: $output\n"
        }

        return result
    }
}