package cc.unitmesh.devti.template

import cc.unitmesh.cf.core.llms.LlmMsg
import cc.unitmesh.devti.custom.team.TeamPromptsBuilder
import cc.unitmesh.devti.settings.AutoDevSettingsState
import cc.unitmesh.devti.template.context.EmptyContext
import cc.unitmesh.devti.template.context.TemplateContext
import cc.unitmesh.template.TemplateRoleSplitter
import com.intellij.openapi.project.ProjectManager
import org.apache.velocity.VelocityContext
import org.apache.velocity.app.Velocity
import java.io.StringWriter
import java.nio.charset.Charset

const val ROOT = "genius"
const val GENIUS_SRE = "/sre"
const val GENIUS_MIGRATION = "/migration"
const val GENIUS_SQL = "/sql"
const val GENIUS_PAGE = "/page"
const val GENIUS_PRACTISES = "/practises"
const val GENIUS_CODE = "/code"
const val GENIUS_CICD = "/cicd"
const val GENIUS_ERROR = "/error"

class TemplateRender(private val pathPrefix: String) {
    private val velocityContext = VelocityContext()
    private val splitter = TemplateRoleSplitter()
    var context: TemplateContext = EmptyContext()
    /**
     * Action is also a context, but with custom methods.
     */
    var actions: Any = ""

    /**
     * Retrieves the template for a given filename.
     *
     * @param filename the name of the file for which the template is requested
     * @return the template string for the specified filename, or the default template if no override is found
     */
    fun getTemplate(filename: String): String {
        val overrideTemplate = ProjectManager.getInstance().openProjects.firstOrNull()?.let {
            TeamPromptsBuilder(it).overrideTemplate(pathPrefix, filename)
        }

        return overrideTemplate ?: retrieveDefaultTemplate(filename)
    }

    // cache templates
    private val templateCache = mutableMapOf<String, String>()

    /**
     * Retrieves the template content from the specified file.
     *
     * @param filename the name of the file containing the template
     * @return the content of the template as a string
     * @throws TemplateNotFoundError if the specified file cannot be found
     */
    private fun retrieveDefaultTemplate(filename: String): String {
        if (templateCache.containsKey(filename)) {
            return templateCache[filename]!!
        }

        val path = getDefaultFilePath(filename)
        val resourceUrl = javaClass.classLoader.getResource(path) ?: throw TemplateNotFoundError(path)
        val bytes = resourceUrl.readBytes()

        val string = String(bytes, Charset.forName("UTF-8"))

        templateCache[filename] = string
        return string
    }

    private fun getDefaultFilePath(filename: String): String {
        val languagePrefix = "$ROOT/${AutoDevSettingsState.language}$pathPrefix".trimEnd('/')
        val path = "$languagePrefix/$filename"
        if (javaClass.classLoader.getResource(path) != null) {
            return path
        }

        val defaultLanguagePrefix = "$ROOT/en/$pathPrefix".trimEnd('/')
        return "$defaultLanguagePrefix/$filename"
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

    fun renderTemplate(template: String, customContext: Any? = null): String {
        val oldContextClassLoader = Thread.currentThread().getContextClassLoader()
        Thread.currentThread().setContextClassLoader(TemplateRender::class.java.getClassLoader())

        if (customContext != null) {
            velocityContext.put("context", customContext)
        } else {
            velocityContext.put("context", this.context)
        }

        val sw = StringWriter()
        Velocity.evaluate(velocityContext, sw, "#" + this.javaClass.name, template)
        val result = sw.toString()

        Thread.currentThread().setContextClassLoader(oldContextClassLoader)

        return result
    }

    fun addVariable(key: String, value: String) {
        velocityContext.put(key, value)
    }
}

class TemplateNotFoundError(path: String) : Exception("Prompt not found at path: $path")
