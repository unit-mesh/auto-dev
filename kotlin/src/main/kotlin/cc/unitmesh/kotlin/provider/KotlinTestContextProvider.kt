package cc.unitmesh.kotlin.provider

import cc.unitmesh.devti.gui.chat.ChatActionType
import cc.unitmesh.devti.provider.context.ChatCreationContext
import cc.unitmesh.idea.provider.JavaTestContextProvider
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.KotlinLanguage

class KotlinTestContextProvider : JavaTestContextProvider() {
    override fun langFileSuffix(): String = ".kt"

    override fun isApplicable(project: Project, creationContext: ChatCreationContext): Boolean {
        return creationContext.action == ChatActionType.WRITE_TEST && creationContext.sourceFile?.language is KotlinLanguage
    }
}
