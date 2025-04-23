package cc.unitmesh.devti.language.run.runner

import cc.unitmesh.devti.custom.team.InteractionType
import cc.unitmesh.devti.language.ast.config.DevInActionLocation
import com.intellij.execution.ui.ConsoleView
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import kotlinx.coroutines.flow.Flow

data class LocationInteractionContext(
    val location: DevInActionLocation,
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
     * the [com.phodal.shirecore.llm.ChatMessage]
     */
    val prompt: String,

    val selectElement: PsiElement? = null,

    /**
     * the console view
     */
    val console: ConsoleView?,
)