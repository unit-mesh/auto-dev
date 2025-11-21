package cc.unitmesh.viewer.web

/**
 * JVM implementation: Load viewer HTML from resources
 */
actual fun getViewerHtml(): String {
    return try {
        val resource = object {}.javaClass.getResource("/viewer.html")
        
        if (resource != null) {
            val html = resource.readText()
            html
        } else {
            getFallbackHtml()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        getFallbackHtml()
    }
}

/**
 * Fallback HTML if resources cannot be loaded
 */
private fun getFallbackHtml(): String {
    return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>AutoDev Viewer (Fallback)</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', sans-serif;
            background: #1e1e1e;
            color: #d4d4d4;
            overflow: hidden;
        }
        #container {
            width: 100vw;
            height: 100vh;
            display: flex;
            flex-direction: column;
        }
        #editor {
            flex: 1;
            width: 100%;
            height: 100%;
            padding: 20px;
            overflow: auto;
        }
        pre {
            white-space: pre-wrap;
            word-wrap: break-word;
            font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
            font-size: 13px;
            line-height: 1.5;
        }
        .error {
            color: #f48771;
            padding: 40px;
            text-align: center;
        }
    </style>
</head>
<body>
    <div id="container">
        <div id="editor">
            <div style="padding: 40px; text-align: center; color: #858585;">
                <div style="font-size: 24px; margin-bottom: 16px;">⚠️</div>
                <div>Fallback Viewer Mode</div>
                <div style="font-size: 12px; margin-top: 8px;">Monaco Editor resources not loaded</div>
            </div>
        </div>
    </div>

    <script>
        console.log('[Viewer HTML - Fallback] Fallback viewer initialized');
        
        window.showContent = function(jsonString) {
            console.log('[Viewer HTML - Fallback] showContent() called');
            try {
                const request = JSON.parse(jsonString);
                console.log('[Viewer HTML - Fallback] Request:', {
                    type: request.type,
                    language: request.language,
                    contentLength: request.content ? request.content.length : 0
                });
                
                const editorDiv = document.getElementById('editor');
                
                if (request.type === 'CODE') {
                    const escapedContent = escapeHtml(request.content);
                    editorDiv.innerHTML = '<pre>' + escapedContent + '</pre>';
                    console.log('[Viewer HTML - Fallback] Content displayed');
                } else {
                    editorDiv.innerHTML = '<div class="error">Unsupported content type: ' + 
                        request.type + '</div>';
                }
            } catch (e) {
                console.error('[Viewer HTML - Fallback] Error:', e);
                document.getElementById('editor').innerHTML = 
                    '<div class="error">Error: ' + e.message + '</div>';
            }
        };
        
        window.clearContent = function() {
            document.getElementById('editor').innerHTML = '';
        };
        
        function escapeHtml(text) {
            const div = document.createElement('div');
            div.textContent = text;
            return div.innerHTML;
        }
        
        console.log('[Viewer HTML - Fallback] Ready to receive content');
    </script>
</body>
</html>
    """.trimIndent()
}


