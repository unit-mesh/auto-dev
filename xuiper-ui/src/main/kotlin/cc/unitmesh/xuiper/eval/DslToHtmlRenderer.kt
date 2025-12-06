package cc.unitmesh.xuiper.eval

import cc.unitmesh.xuiper.dsl.NanoDSL
import cc.unitmesh.xuiper.parser.ParseResult
import cc.unitmesh.xuiper.render.HtmlRenderer
import java.io.File

/**
 * Renders NanoDSL files to HTML files.
 * 
 * Run with: ./gradlew :xuiper-ui:renderHtml
 * Or: ./gradlew :xuiper-ui:renderHtml -PdslDir=testcases/expect -PoutputDir=testcases/html
 */
fun main(args: Array<String>) {
    val inputDir = args.getOrNull(0) ?: "testcases/actual/integration"
    val outputDir = args.getOrNull(1) ?: "testcases/html/integration"
    
    val inputDirectory = File(inputDir)
    val outputDirectory = File(outputDir)
    
    if (!inputDirectory.exists() || !inputDirectory.isDirectory) {
        println("❌ Input directory not found: ${inputDirectory.absolutePath}")
        return
    }
    
    // Create output directory
    outputDirectory.mkdirs()
    
    val files = inputDirectory.listFiles { f -> f.extension == "nanodsl" }?.sortedBy { it.name } ?: emptyList()
    
    if (files.isEmpty()) {
        println("⚠️  No .nanodsl files found in $inputDir")
        return
    }
    
    println("=" .repeat(70))
    println("NanoDSL to HTML Renderer")
    println("=" .repeat(70))
    println("Input:  ${inputDirectory.absolutePath}")
    println("Output: ${outputDirectory.absolutePath}")
    println("Files:  ${files.size}")
    println("-".repeat(70))
    
    val renderer = HtmlRenderer()
    var success = 0
    var failed = 0
    
    files.forEach { file ->
        val source = file.readText()
        val result = NanoDSL.parseResult(source)
        
        when (result) {
            is ParseResult.Success -> {
                try {
                    val ir = NanoDSL.toIR(result.ast)
                    val html = renderer.render(ir)
                    
                    val outputFile = File(outputDirectory, file.nameWithoutExtension + ".html")
                    outputFile.writeText(html)
                    
                    println("✅ ${file.name} → ${outputFile.name}")
                    println("   Size: ${html.length} bytes")
                    success++
                } catch (e: Exception) {
                    println("❌ ${file.name} - Render failed: ${e.message}")
                    failed++
                }
            }
            is ParseResult.Failure -> {
                println("❌ ${file.name} - Parse failed")
                result.errors.forEach { error ->
                    println("   Line ${error.line}: ${error.message}")
                }
                failed++
            }
        }
    }
    
    println("-".repeat(70))
    println("Summary: $success rendered, $failed failed")
    println("=".repeat(70))
    
    if (success > 0) {
        println("\n📂 HTML files saved to: ${outputDirectory.absolutePath}")
        println("   Open in browser: file://${outputDirectory.absolutePath}/index.html")
        
        // Generate index.html
        generateIndexHtml(outputDirectory, files.mapNotNull { file ->
            val outputFile = File(outputDirectory, file.nameWithoutExtension + ".html")
            if (outputFile.exists()) file.nameWithoutExtension else null
        })
    }
}

private fun generateIndexHtml(outputDir: File, fileNames: List<String>) {
    val indexHtml = buildString {
        append("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>NanoDSL Preview Gallery</title>
                <style>
                    body { 
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                        max-width: 1200px;
                        margin: 0 auto;
                        padding: 20px;
                        background: #f5f5f5;
                    }
                    h1 { color: #333; margin-bottom: 20px; }
                    .grid { 
                        display: grid;
                        grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
                        gap: 20px;
                    }
                    .card {
                        background: white;
                        border-radius: 8px;
                        padding: 16px;
                        box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                    }
                    .card h3 { margin: 0 0 12px 0; color: #6200EE; }
                    .card a {
                        display: inline-block;
                        padding: 8px 16px;
                        background: #6200EE;
                        color: white;
                        text-decoration: none;
                        border-radius: 4px;
                        font-size: 14px;
                    }
                    .card a:hover { background: #3700B3; }
                </style>
            </head>
            <body>
                <h1>🎨 NanoDSL Preview Gallery</h1>
                <p>Generated from integration test outputs</p>
                <div class="grid">
        """.trimIndent())
        
        fileNames.forEach { name ->
            append("""
                    <div class="card">
                        <h3>$name</h3>
                        <a href="$name.html" target="_blank">View Preview →</a>
                    </div>
            """.trimIndent())
        }
        
        append("""
                </div>
            </body>
            </html>
        """.trimIndent())
    }
    
    File(outputDir, "index.html").writeText(indexHtml)
    println("   Generated index.html with ${fileNames.size} links")
}

