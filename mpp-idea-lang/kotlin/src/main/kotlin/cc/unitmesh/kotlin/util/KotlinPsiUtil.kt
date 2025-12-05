package cc.unitmesh.kotlin.util

import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtNamedFunction

object KotlinPsiUtil {
    fun getFunctions(kotlinClass: KtClassOrObject): List<KtFunction> {
        return kotlinClass.getDeclarations().filterIsInstance<KtFunction>()
    }

    fun getClasses(ktFile: KtFile): List<KtClassOrObject> {
        return ktFile.declarations.filterIsInstance<KtClassOrObject>()
    }

    fun signatureString(signatureString: KtNamedFunction): String {
        val bodyBlockExpression = signatureString.bodyBlockExpression
        val startOffsetInParent = if (bodyBlockExpression != null) {
            bodyBlockExpression.startOffsetInParent
        } else {
            val bodyExpression = signatureString.bodyExpression
            bodyExpression?.startOffsetInParent ?: signatureString.textLength
        }

        val text = signatureString.text
        val substring = text.substring(0, startOffsetInParent)
        return substring.replace('\n', ' ').trim()
    }

}