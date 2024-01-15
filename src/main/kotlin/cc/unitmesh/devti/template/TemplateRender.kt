package cc.unitmesh.devti.template

import cc.unitmesh.cf.core.llms.LlmMsg
import cc.unitmesh.template.TemplateRoleSplitter
import org.apache.velocity.VelocityContext
import org.apache.velocity.app.Velocity
import java.io.StringWriter
import java.nio.charset.Charset

class TemplateRender(pathPrefix: String) {
    private val defaultPrefix: String = pathPrefix.trimEnd('/')
    private val velocityContext = VelocityContext()
    private val splitter = TemplateRoleSplitter()
    var context: Any = ""

    /**
     * Retrieves the template content from the specified file.
     *
     * @param filename the name of the file containing the template
     * @return the content of the template as a string
     * @throws TemplateNotFoundError if the specified file cannot be found
     */
    fun getTemplate(filename: String): String {
        val path = "$defaultPrefix/$filename"
        val resourceUrl = javaClass.classLoader.getResource(path) ?: throw TemplateNotFoundError(path)
        val bytes = resourceUrl.readBytes()
        return String(bytes, Charset.forName("UTF-8"))
    }

    fun buildMsgs(prompt: String): List<LlmMsg.ChatMessage> {
        val msgs = splitter.split(prompt)
        val messages = LlmMsg.fromMap(msgs).toMutableList()

        val oldContextClassLoader = Thread.currentThread().getContextClassLoader()
        Thread.currentThread().setContextClassLoader(TemplateRender::class.java.getClassLoader())

        messages.map {
            velocityContext.put("context", context)
            val sw = StringWriter()
            Velocity.evaluate(velocityContext, sw, "#" + this.javaClass.name, it.content)
            it.content = sw.toString()
        }

        Thread.currentThread().setContextClassLoader(oldContextClassLoader)

        return messages
    }
}

class TemplateNotFoundError(path: String) : Exception("Prompt not found at path: $path")
