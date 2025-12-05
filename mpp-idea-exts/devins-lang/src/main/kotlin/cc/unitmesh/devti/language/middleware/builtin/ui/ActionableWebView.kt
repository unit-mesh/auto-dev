package cc.unitmesh.devti.language.middleware.builtin.ui

import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefJSQuery
import java.util.concurrent.CompletableFuture

class ActionableWebView(private val browser: JBCefBrowser) {
    private var future: CompletableFuture<Void> = CompletableFuture.completedFuture(null)
    private var onErrorAction: (() -> Unit)? = null

    fun open(url: String): ActionableWebView {
        future = future.thenRun {
            browser.loadURL(url)
        }
        return this
    }

    fun waitFor(selector: String, timeout: Long): ActionableWebView {
        future = future.thenCompose {
            val result = CompletableFuture<Void>()
            val jsQuery = JBCefJSQuery.create(browser) // 1

            jsQuery.addHandler { response ->
                if (response == "success") {
                    result.complete(null)
                } else {
                    result.completeExceptionally(Exception("Timeout waiting for $selector"))
                }
                null
            }

            val script = """
                (function() {
                    var interval = setInterval(function() {
                        if (document.querySelector('$selector') !== null) {
                            clearInterval(interval);
                            window['_java_callback_${jsQuery}']('success');
                        }
                    }, 100);
                    setTimeout(function() {
                        clearInterval(interval);
                        window['_java_callback_${jsQuery}']('error');
                    }, $timeout);
                })();
            """

            val wrappedScript = """
                window['_java_callback_${jsQuery}'] = function(response) {
                    ${jsQuery.inject("response")}
                };
                $script
            """

            browser.cefBrowser.executeJavaScript(wrappedScript, browser.cefBrowser.url, 0)

            result
        }.exceptionally { throwable ->
            onErrorAction?.invoke()
            null
        }
        return this
    }

    fun input(selector: String, text: String): ActionableWebView {
        future = future.thenRun {
            val script = "document.querySelector('$selector').value = '$text';"
            browser.cefBrowser.executeJavaScript(script, browser.cefBrowser.url, 0)
        }
        return this
    }

    fun click(selector: String): ActionableWebView {
        future = future.thenRun {
            val script = "document.querySelector('$selector').click();"
            browser.cefBrowser.executeJavaScript(script, browser.cefBrowser.url, 0)
        }
        return this
    }

    fun onError(action: () -> Unit): ActionableWebView {
        onErrorAction = action
        return this
    }

    companion object {
        fun create(browser: JBCefBrowser): ActionableWebView {
//            // Ensure JCEF is initialized
//            JBCefApp.getInstance()
//
//            // Create the browser component
//            val browser = JBCefBrowser()
//
//            // Add the browser to the tool window content
//            val contentFactory = ContentFactory.getInstance()
//            val content = contentFactory.createContent(browser.component, "", false)
//
//            // Start the action chain as per your DSL
            return ActionableWebView(browser)
        }
    }
}
