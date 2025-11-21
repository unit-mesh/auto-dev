package cc.unitmesh.viewer.web

/**
 * JVM implementation: Load mermaid HTML from resources
 */
actual fun getMermaidHtml(): String {
    println("[MermaidHtml.jvm] Loading mermaid.html from resources")
    
    return try {
        // Try to load from resources
        val resource = object {}.javaClass.getResource("/mermaid.html")
        
        if (resource != null) {
            val html = resource.readText()
            println("[MermaidHtml.jvm] Successfully loaded mermaid.html (${html.length} chars)")
            html
        } else {
            println("[MermaidHtml.jvm] WARNING: mermaid.html not found in resources, using fallback")
            getFallbackMermaidHtml()
        }
    } catch (e: Exception) {
        println("[MermaidHtml.jvm] ERROR loading mermaid.html: ${e.message}")
        e.printStackTrace()
        getFallbackMermaidHtml()
    }
}

/**
 * Fallback Mermaid HTML if resources cannot be loaded
 */
private fun getFallbackMermaidHtml(): String {
    return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Mermaid Viewer (Fallback)</title>
    <style>
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
            background: #1e1e1e;
            color: #d4d4d4;
            display: flex;
            align-items: center;
            justify-content: center;
            height: 100vh;
            margin: 0;
        }
        .error {
            text-align: center;
            color: #f48771;
        }
    </style>
</head>
<body>
    <div class="error">
        <div style="font-size: 48px;">⚠️</div>
        <div>Failed to load mermaid.html</div>
        <div style="font-size: 12px; margin-top: 8px;">Fallback mode</div>
    </div>
    <script>
        console.log('[Mermaid Fallback] Fallback HTML loaded');
        window.renderMermaid = function(code) {
            console.error('[Mermaid Fallback] Cannot render - using fallback HTML');
        };
    </script>
</body>
</html>
    """.trimIndent()
}

