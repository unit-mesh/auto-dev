package cc.unitmesh.devti.custom.variable

import com.intellij.openapi.application.ReadAction
import com.intellij.psi.PsiElement
import com.intellij.temporary.similar.chunks.SimilarChunksWithPaths

class SimilarChunkVariableResolver(val element: PsiElement) : VariableResolver {
    override val type: CustomIntentionVariableType get() = CustomIntentionVariableType.SIMILAR_CHUNK

    override fun resolve(): String {
        return ReadAction.compute<String, Throwable> {
            SimilarChunksWithPaths.createQuery(element, 256)
        }
    }
}