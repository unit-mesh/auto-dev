package cc.unitmesh.devti.practise

import cc.unitmesh.devti.AutoDevIcons
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.completion.PrefixMatcher
import com.intellij.codeInsight.lookup.*
import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.codeInsight.template.impl.TemplateManagerImpl
import com.intellij.openapi.editor.Editor
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

//        lookupImpl.addLookupListener(RenameLookupListener())
        val suggestionNames = listOf("suggestion1", "suggestion2", "suggestion3")
        suggestionNames.map {
            lookupImpl.addItem(RenameLookupElement(it), PrefixMatcher.ALWAYS_TRUE)
        }
    }
}

class RenameLookupElement(val name: String) : LookupElement() {
    override fun getLookupString(): String = name
    override fun renderElement(presentation: LookupElementPresentation) {
        presentation.icon = AutoDevIcons.Idea
        super.renderElement(presentation)
    }
}

class RenameLookupListener : LookupListener {

}
