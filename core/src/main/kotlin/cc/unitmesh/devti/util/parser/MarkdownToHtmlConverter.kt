package cc.unitmesh.devti.util.parser

import org.intellij.markdown.IElementType
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser

// https://github.com/JetBrains/markdown/issues/72
private val embeddedHtmlType = IElementType("ROOT")

/**
 * Creates a GFMFlavourDescriptor instance that's compatible with the current IntelliJ version
 */
private fun createGFMFlavourDescriptor(): GFMFlavourDescriptor {
    return try {
        // Try the constructor without parameters (newer versions)
        GFMFlavourDescriptor()
    } catch (e: NoSuchMethodError) {
        try {
            // Try with parameters for version 223
            GFMFlavourDescriptor::class.java.getConstructor(Boolean::class.java)
                .newInstance(false)
        } catch (e: Exception) {
            try {
                // Try another possible constructor signature
                GFMFlavourDescriptor::class.java.getConstructor(Boolean::class.java, Boolean::class.java)
                    .newInstance(false, false)
            } catch (e: Exception) {
                // Last resort - try using Kotlin reflection to access the class
                @Suppress("UNCHECKED_CAST")
                val constructorFun = GFMFlavourDescriptor::class.constructors.firstOrNull()
                    ?: throw RuntimeException("Cannot find compatible GFMFlavourDescriptor constructor")
                
                val paramCount = constructorFun.parameters.size
                when (paramCount) {
                    0 -> constructorFun.call()
                    1 -> constructorFun.call(false)
                    2 -> constructorFun.call(false, false)
                    else -> throw RuntimeException("Unsupported GFMFlavourDescriptor constructor")
                }
            }
        }
    }
}

fun convertMarkdownToHtml(markdownText: String): String {
    try {
        val flavour = createGFMFlavourDescriptor()
        val parsedTree = MarkdownParser(flavour).parse(embeddedHtmlType, markdownText)
        return HtmlGenerator(markdownText, parsedTree, flavour, false).generateHtml()
    } catch (e: Exception) {
        return markdownText
    }
}
