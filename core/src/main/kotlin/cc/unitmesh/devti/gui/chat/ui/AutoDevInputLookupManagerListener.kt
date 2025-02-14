package cc.unitmesh.devti.gui.chat.ui

import cc.unitmesh.devti.gui.chat.AutoDevFileLookupElement
import com.intellij.codeInsight.lookup.Lookup
import com.intellij.codeInsight.lookup.LookupEvent
import com.intellij.codeInsight.lookup.LookupListener
import com.intellij.codeInsight.lookup.LookupManagerListener
import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager

class AutoDevInputLookupManagerListener(
    private val project: Project,
    private val callback: ((PsiFile) -> Unit)? = null,
) : LookupManagerListener {
    override fun activeLookupChanged(oldLookup: Lookup?, newLookup: Lookup?) {
        if (newLookup !is LookupImpl) return

        newLookup.addLookupListener(object : LookupListener {
            override fun itemSelected(event: LookupEvent) {
                if (event.item !is AutoDevFileLookupElement<*>) return

                val lookupElement = event.item as AutoDevFileLookupElement<*>

                runReadAction {
                    val file = lookupElement.getFile()
                    val psiFile = PsiManager.getInstance(project).findFile(file)
                    if (psiFile != null) {
                        callback?.invoke(psiFile)
                    }
                }
            }
        })
    }
}