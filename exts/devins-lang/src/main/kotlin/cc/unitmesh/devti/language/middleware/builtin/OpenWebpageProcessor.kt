package cc.unitmesh.devti.language.middleware.builtin

import com.intellij.execution.ui.ConsoleView
import com.intellij.ide.DataManager
import com.intellij.ide.IdeBundle
import com.intellij.ide.browsers.BrowserLauncher
import com.intellij.ide.browsers.OpenInBrowserRequest
import com.intellij.ide.browsers.WebBrowserService
import com.intellij.ide.browsers.WebBrowserUrlProvider
import com.intellij.ide.browsers.actions.findUsingBrowser
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import cc.unitmesh.devti.devins.post.PostProcessor
import cc.unitmesh.devti.devins.post.PostProcessorContext
import cc.unitmesh.devti.devins.post.PostProcessorType

class OpenWebpageProcessor : PostProcessor {
    override val processorName: String get() = PostProcessorType.OpenWebpage.handleName
    override val description: String get() = "`openWebpage` will open the generated HTML in the browser"

    override fun isApplicable(context: PostProcessorContext): Boolean {
        return context.genText?.contains("<html") ?: false
    }

    override fun execute(project: Project, context: PostProcessorContext, console: ConsoleView?, args: List<Any>): Any {
        val dataContext = DataManager.getInstance().dataContextFromFocusAsync.blockingGet(10000)
            ?: throw IllegalStateException("No data context")
        val editor = CommonDataKeys.EDITOR.getData(dataContext) ?: return ""
        val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.document)
            ?: throw IllegalStateException("No PSI file")

        val request = object : OpenInBrowserRequest(psiFile, true) {
            private val lazyElement by lazy { file.findElementAt(editor.caretModel.offset) }

            override val element: PsiElement
                get() = lazyElement ?: file
        }

        try {
            val browser = findUsingBrowser()
            val urls = WebBrowserService.getInstance().getUrlsToOpen(request, true)
            if (!urls.isEmpty()) {
                val url = urls.first()
                runInEdt {
                    FileDocumentManager.getInstance().saveAllDocuments()
                }

                BrowserLauncher.instance.browse(url.toExternalForm(), browser, request.project)
            }
        } catch (e: WebBrowserUrlProvider.BrowserException) {
            Messages.showErrorDialog(e.message, IdeBundle.message("browser.error"))
        } catch (e: Exception) {
            logger<OpenWebpageProcessor>().warn(e)
        }

        return ""
    }
}
