package cc.unitmesh.devti.language.middleware.builtin

import com.intellij.execution.ui.ConsoleView
import com.intellij.ide.DataManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.dsl.builder.*
import cc.unitmesh.devti.devins.post.PostProcessor
import cc.unitmesh.devti.devins.post.PostProcessorContext
import cc.unitmesh.devti.devins.post.PostProcessorType
import cc.unitmesh.devti.language.middleware.builtin.ui.WebViewWindow
import cc.unitmesh.devti.llms.LlmFactory
import cc.unitmesh.devti.llms.cancelHandler
import cc.unitmesh.devti.util.AutoDevCoroutineScope
import cc.unitmesh.devti.util.parser.CodeFence
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent

class ShowWebviewProcessor : PostProcessor {
    override val processorName: String get() = PostProcessorType.ShowWebview.handleName
    override val description: String get() = "`showWebview` will show the webview for the content if it's html"

    private var continueMessage: String = ""
    private var webview: WebViewWindow? = null

    override fun isApplicable(context: PostProcessorContext): Boolean {
        return true
    }

    override fun execute(project: Project, context: PostProcessorContext, console: ConsoleView?, args: List<Any>): Any {
        var html: String? = (context.pipeData["output"])?.toString() ?: context.genText

        val dataContext = DataManager.getInstance().dataContextFromFocusAsync.blockingGet(10000)
            ?: throw IllegalStateException("No data context")

        var keyAdapter: WebviewKeyAdapter? = null

        runInEdt {
            webview = WebViewWindow()
            val component = webview!!.apply { loadHtml(html ?: "") }.component

            val panel = panel {
                row {
                    cell(component)
                }
                row {
                    textArea()
                        .align(Align.FILL)
                        .bindText(::continueMessage)
                        .applyToComponent {
                            font = EditorFontType.getGlobalPlainFont()
                            keyAdapter = WebviewKeyAdapter(project, this, webview, html) {
                                html = it
                                continueMessage = ""
                            }

                            addKeyListener(keyAdapter)
                        }
                }
            }

            val popup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(panel, null)
                .setResizable(true)
                .setMovable(true)
                .setTitle("Preview")
                .setFocusable(true)
                .setRequestFocus(true)
                .createPopup()
                .also { keyAdapter?.popup = it }

            popup.showInBestPositionFor(dataContext)
        }

        return ""
    }
}

class WebviewKeyAdapter(
    val project: Project, val textarea: JBTextArea, val currentWebview: WebViewWindow?,
    val html: String?,
    val onEnter: (String) -> Unit,
) : KeyAdapter() {
    var popup: JBPopup? = null
    override fun keyPressed(e: KeyEvent) {
        if (e.keyCode == KeyEvent.VK_ENTER) {
            textarea.isEditable = false
            currentWebview?.loadHtml("Processing...")
            var result = ""

            AutoDevCoroutineScope.scope(project).launch {
                val flow = LlmFactory.create(project)
                    ?.stream(
                        "According user input to modify code, return new code." +
                                " Use input: ${textarea.text}\nCode: \n```html\n$html\n```" +
                                "\n" +
                                "Return new code: ",
                        "",
                        false
                    )!!

                var popupListener: JBPopupListener? = null

                runBlocking {
                    flow.cancelHandler {
                        if (popup?.isDisposed == true) it.invoke("This popup has been disposed")
                        else popup?.addListener(object : JBPopupListener {
                            override fun onClosed(event: LightweightWindowEvent) {
                                it.invoke("This popup has been closed")
                            }
                        }.also { popupListener = it })
                    }.cancellable().collect {
                        result += it
                    }

                    popupListener?.run { popup?.removeListener(this) }
                    textarea.isEditable = true
                }

                val newHtml = CodeFence.parse(result).text
                logger<ShowWebviewProcessor>().info("Result: $result")
                runInEdt {
                    currentWebview?.loadHtml(newHtml)
                }

                onEnter(newHtml)
                textarea.text = ""
            }
        }
    }
}

