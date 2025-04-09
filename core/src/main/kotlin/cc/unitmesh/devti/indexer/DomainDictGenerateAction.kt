package cc.unitmesh.devti.indexer

import cc.unitmesh.devti.settings.locale.LanguageChangedCallback.presentationText
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class DomainDictGenerateAction: AnAction() {
    init{
        presentationText("indexer.generate.domain", templatePresentation)
    }

    override fun actionPerformed(event: AnActionEvent) {
        /// todo: add load source content file and call llm to gerneate to csv
        TODO("Not yet implemented")
    }
}