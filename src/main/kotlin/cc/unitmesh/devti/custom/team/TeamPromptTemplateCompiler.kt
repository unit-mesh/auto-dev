package cc.unitmesh.devti.custom.team

import com.intellij.lang.Language
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.NlsSafe
import org.apache.velocity.VelocityContext
import org.apache.velocity.app.Velocity
import java.io.StringWriter

class TeamPromptTemplateCompiler(val language: Language) {
    private val velocityContext = VelocityContext()
    companion object {
        val log = logger<TeamPromptTemplateCompiler>()
    }

    fun compile(template: String): String {
        configForLanguage()
        configForFramework()

        // without the following class loader initialization, I get the
        // following exception when running as Eclipse plugin:
        // org.apache.velocity.exception.VelocityException: The specified
        // class for ResourceManager
        // (org.apache.velocity.runtime.resource.ResourceManagerImpl) does not
        // implement org.apache.velocity.runtime.resource.ResourceManager;
        // Velocity is not initialized correctly.
        // without the following class loader initialization, I get the
        // following exception when running as Eclipse plugin:
        // org.apache.velocity.exception.VelocityException: The specified
        // class for ResourceManager
        // (org.apache.velocity.runtime.resource.ResourceManagerImpl) does not
        // implement org.apache.velocity.runtime.resource.ResourceManager;
        // Velocity is not initialized correctly.
        val oldContextClassLoader = Thread.currentThread().getContextClassLoader()
        Thread.currentThread().setContextClassLoader(TeamPromptTemplateCompiler::class.java.getClassLoader())

        val sw = StringWriter()
        try {
            Velocity.evaluate(velocityContext, sw, "#" + this.javaClass.name, template)
        } catch (e: Exception) {
            log.error("Failed to compile template: $template", e)
            sw.write(template)
        }

        Thread.currentThread().setContextClassLoader(oldContextClassLoader)
        return sw.toString()
    }

    private fun configForFramework() {
//        frameworkContext
    }

    private fun configForLanguage() {
        // frameworkContext
        velocityContext.put("language", language.displayName)
        velocityContext.put(
            "commentSymbol", when (language.displayName.lowercase()) {
                "java", "kotlin" -> "//"
                "python" -> "#"
                "javascript" -> "//"
                "typescript" -> "//"
                "go" -> "//"
                else -> "-"
            }
        )
    }

    fun set(key: String, value: @NlsSafe String) {
        velocityContext.put(key, value)
    }
}