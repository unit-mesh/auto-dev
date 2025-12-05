package cc.unitmesh.devti.devins.provider.location

import cc.unitmesh.devti.custom.team.InteractionType
import cc.unitmesh.devti.devins.ShireActionLocation
import com.intellij.execution.ui.ConsoleView
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import kotlinx.coroutines.flow.Flow

data class LocationInteractionContext(
    val location: ShireActionLocation,
    /**
     * the interaction type
     */
    val interactionType: InteractionType,
    /**
     * the LLM generate text stream, which can be used for [InteractionType.AppendCursorStream]
     */
    val streamText: Flow<String>? = null,

    val editor: Editor?,

    val project: Project,

    /**
     * the [cc.unitmesh.shirecore.llm.ChatMessage]
     */
    val prompt: String,

    val selectElement: PsiElement? = null,

    /**
     * the console view
     */
    val console: ConsoleView?,
)