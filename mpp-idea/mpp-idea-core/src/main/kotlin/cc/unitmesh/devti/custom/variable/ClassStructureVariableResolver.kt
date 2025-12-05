package cc.unitmesh.devti.custom.variable

import cc.unitmesh.devti.context.ClassContextProvider
import com.intellij.psi.PsiElement

class ClassStructureVariableResolver(val element: PsiElement) : VariableResolver {
    override val type: CustomResolvedVariableType = CustomResolvedVariableType.METHOD_INPUT_OUTPUT

    override fun resolve(): String {
        val classContext = ClassContextProvider(false).from(element)
        if (classContext.name == null) return ""

        return classContext.format()
    }
}
