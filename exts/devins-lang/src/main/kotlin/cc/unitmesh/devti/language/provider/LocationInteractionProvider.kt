package cc.unitmesh.devti.language.provider

import cc.unitmesh.devti.language.run.runner.LocationInteractionContext
import cc.unitmesh.devti.language.run.runner.PostFunction
import com.intellij.openapi.extensions.ExtensionPointName

/**
 * Interface for managing interactions in different IDE locations.
 * The interactions are categorized into three types:
 * - Terminal: Appends stream in the IDE terminal
 * - Editor: Appends stream in the IDE editor
 * - CommitPanel: Appends stream in the IDE commit panel
 */
interface LocationInteractionProvider {
    fun isApplicable(context: LocationInteractionContext): Boolean

    fun execute(context: LocationInteractionContext, postExecute: PostFunction)

    companion object {
        private val EP_NAME: ExtensionPointName<LocationInteractionProvider> =
            ExtensionPointName("com.phodal.shireLocationInteraction")

        fun provide(context: LocationInteractionContext): LocationInteractionProvider? {
            return EP_NAME.extensionList.firstOrNull {
                it.isApplicable(context)
            }
        }
    }
}
