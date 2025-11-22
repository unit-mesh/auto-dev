package cc.unitmesh.viewer.web

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewStateWithHTMLData

@Composable
actual fun PlantUmlRenderer(
    code: String,
    isDarkTheme: Boolean,
    modifier: Modifier,
    onRenderComplete: ((success: Boolean, message: String) -> Unit)?
) {
    val escapedCode = code
        .replace("\\", "\\\\")
        .replace("`", "\\`")
        .replace("$", "\\$")
        .replace("\n", "\\n")

    val html = """
        <!DOCTYPE html>
        <html>
        <head>
            <script src="https://cdn.jsdelivr.net/npm/plantuml-encoder@1.4.0/dist/plantuml-encoder.min.js"></script>
            <style>
                body, html { 
                    margin: 0; 
                    padding: 0; 
                    height: 100%; 
                    display: flex; 
                    justify-content: center; 
                    align-items: center; 
                    background-color: ${if (isDarkTheme) "#171717" else "#fafafa"}; 
                }
                img { max-width: 100%; max-height: 100%; object-fit: contain; }
            </style>
        </head>
        <body>
            <img id="diagram" />
            <script>
                try {
                    var encoded = plantumlEncoder.encode(`$escapedCode`);
                    document.getElementById('diagram').src = 'https://www.plantuml.com/plantuml/svg/' + encoded;
                } catch (e) {
                    console.error(e);
                }
            </script>
        </body>
        </html>
    """.trimIndent()

    val webViewState = rememberWebViewStateWithHTMLData(data = html)

    WebView(
        state = webViewState,
        modifier = modifier.fillMaxSize()
    )
}
