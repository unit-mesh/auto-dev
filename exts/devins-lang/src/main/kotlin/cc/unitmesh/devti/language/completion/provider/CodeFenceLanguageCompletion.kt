package cc.unitmesh.devti.language.completion.provider

import cc.unitmesh.devti.markdown.CodeFenceLanguageAliases
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.lang.Language
import com.intellij.lang.LanguageUtil
import com.intellij.ui.DeferredIconImpl
import com.intellij.util.ProcessingContext
import javax.swing.Icon

class CodeFenceLanguageCompletion : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        for (language in LanguageUtil.getInjectableLanguages()) {
            val alias = CodeFenceLanguageAliases.findMainAlias(language.id)
            val handler = LookupElementBuilder.create(alias)
                .withIcon(createLanguageIcon(language))
                .withTypeText(language.displayName, true)
                .withInsertHandler(MyInsertHandler())

            result.addElement(handler)
        }
    }

    private fun createLanguageIcon(language: Language): Icon {
        return DeferredIconImpl(null, language, true) { it.associatedFileType?.icon }
    }

    private class MyInsertHandler : InsertHandler<LookupElement> {
        override fun handleInsert(context: InsertionContext, item: LookupElement) {
            context.document.insertString(context.tailOffset, "\n\n")
            context.editor.caretModel.moveCaretRelatively(1, 0, false, false, false)
        }
    }
}