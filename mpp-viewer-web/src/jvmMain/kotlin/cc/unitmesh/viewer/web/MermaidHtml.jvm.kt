package cc.unitmesh.viewer.web

/**
 * JVM implementation: Load mermaid HTML from resources
 */
actual fun getMermaidHtml(): String {
    return try {
        val resource = object {}.javaClass.getResource("/mermaid.html")
        resource?.readText() ?: getFallbackMermaidHtml()
    } catch (e: Exception) {
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
    <style>
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
            background: transparent;
            color: #f48771;
            display: flex;
            align-items: center;
            justify-content: center;
            height: 100vh;
            margin: 0;
        }
    </style>
</head>
<body>
    <div>Failed to load mermaid.html</div>
    <script>
        window.renderMermaid = function() {};
    </script>
</body>
</html>
    """.trimIndent()
}

