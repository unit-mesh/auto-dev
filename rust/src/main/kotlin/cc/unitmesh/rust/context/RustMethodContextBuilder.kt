package cc.unitmesh.rust.context

import cc.unitmesh.devti.context.MethodContext
import cc.unitmesh.devti.context.builder.MethodContextBuilder
import com.intellij.psi.PsiElement
import org.rust.ide.docs.signature
import org.rust.lang.core.psi.RsFunction
import org.rust.lang.core.types.implLookup

class RustMethodContextBuilder : MethodContextBuilder {
    override fun getMethodContext(
        psiElement: PsiElement,
        includeClassContext: Boolean,
        gatherUsages: Boolean
    ): MethodContext? {
        if (psiElement !is RsFunction) return null

        val text = psiElement.text
        val returnType = psiElement.retType?.text ?: ""
        val language = psiElement.language.displayName

        val signature = StringBuilder()
        psiElement.signature(signature)

        return MethodContext(
            psiElement,
            text,
            psiElement.name,
            signature.toString(),
            null,
            language,
            returnType,
            emptyList(),
            includeClassContext,
            emptyList()
        )
    }
}
