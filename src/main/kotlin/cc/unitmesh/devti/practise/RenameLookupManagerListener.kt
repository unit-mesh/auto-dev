package cc.unitmesh.devti.practise

import com.intellij.codeInsight.lookup.Lookup
import com.intellij.codeInsight.lookup.LookupListener
import com.intellij.codeInsight.lookup.LookupManagerListener
import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.openapi.project.Project

class RenameLookupManagerListener(val project: Project) : LookupManagerListener {
    override fun activeLookupChanged(oldLookup: Lookup?, newLookup: Lookup?) {
        val lookupImpl = newLookup as? LookupImpl ?: return

//        val editor = lookupImpl.editor
//        val activeTemplate = TemplateManager.getInstance(project).getActiveTemplate(editor) ?: return

        val lookupOriginalStart = lookupImpl.lookupOriginalStart
        val startOffset = if (lookupOriginalStart > -1) lookupOriginalStart else 0
        val psiElement = lookupImpl.psiFile?.findElementAt(startOffset)
        val element = psiElement ?: lookupImpl.psiElement

        lookupImpl.addLookupListener(RenameLookupListener())
    }
}

class RenameLookupListener : LookupListener {

}
