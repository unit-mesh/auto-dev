package cc.unitmesh.devti.language

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.psi.PsiElement

class DevtiAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {


    }

    companion object {
        val DEVTI_REGEX = Regex("^//\\sdevti://story/github/\\d+(/.*)?$")
    }
}