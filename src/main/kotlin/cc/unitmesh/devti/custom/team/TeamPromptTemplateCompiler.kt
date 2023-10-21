package cc.unitmesh.devti.custom.team

import cc.unitmesh.devti.gui.chat.ChatActionType
import cc.unitmesh.devti.provider.context.ChatContextProvider
import cc.unitmesh.devti.provider.context.ChatCreationContext
import cc.unitmesh.devti.provider.context.ChatOrigin
import com.intellij.lang.Language
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.NlsSafe
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import kotlinx.coroutines.runBlocking
import org.apache.velocity.VelocityContext
import org.apache.velocity.app.Velocity
import java.io.StringWriter
import com.intellij.psi.PsiNameIdentifierOwner

class TeamPromptTemplateCompiler(val language: Language, val file: PsiFile, val element: PsiElement?) {
    private val velocityContext = VelocityContext()

    companion object {
        val log = logger<TeamPromptTemplateCompiler>()
    }

    fun compile(template: String): String {
        velocityContext.put("fileName", file.name)
        velocityContext.put("filePath", file.virtualFile?.path ?: "")
        velocityContext.put("methodName", when(element) {
            is PsiNameIdentifierOwner -> element.nameIdentifier?.text ?: ""
            else -> ""
        })

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
            velocityContext.put("context", SimpleTeamContextProvider(element))
            Velocity.evaluate(velocityContext, sw, "#" + this.javaClass.name, template)
        } catch (e: Exception) {
            log.error("Failed to compile template: $template", e)
            sw.write(template)
        }

        Thread.currentThread().setContextClassLoader(oldContextClassLoader)
        return sw.toString()
    }

    private fun configForFramework() {
        runBlocking {
            val context = ChatCreationContext(
                ChatOrigin.Intention,
                ChatActionType.CUSTOM_ACTION,
                file,
                listOf(),
                null
            )
            val collectChatContextList = ChatContextProvider.collectChatContextList(file.project, context)
            velocityContext.put("frameworkContext", collectChatContextList.joinToString("\n") {
                it.text
            })
        }
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