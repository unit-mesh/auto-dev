package cc.unitmesh.devti.template

import cc.unitmesh.cf.core.llms.LlmMsg
import cc.unitmesh.template.TemplateRoleSplitter
import cc.unitmesh.template.VelocityCompiler
import java.nio.charset.Charset

class TemplateRender(pathPrefix: String) {
    private val classLoader: ClassLoader = this.javaClass.classLoader
    private val defaultPrefix: String = pathPrefix.trimEnd('/')
    protected val template = VelocityCompiler()
    protected val splitter = TemplateRoleSplitter()
    protected val context: TemplateContext = object : TemplateContext {}

    fun getTemplate(filename: String): String {
        val path = "$defaultPrefix/$filename"
        val resourceUrl = classLoader.getResource(path) ?: throw TemplateNotFoundError(path)
        val bytes = resourceUrl.readBytes()
        return String(bytes, Charset.forName("UTF-8"))
    }


    fun create(prompt: String): List<LlmMsg.ChatMessage> {
        val msgs = splitter.split(prompt)
        val messages = LlmMsg.fromMap(msgs).toMutableList()

        messages.map {
            if (it.role == LlmMsg.ChatRole.User) {
                template.append("context", context)
                it.content = template.compileToString(it.content)
            }
        }

        return messages
    }

}

class TemplateNotFoundError(path: String) : Exception("Prompt not found at path: $path")
