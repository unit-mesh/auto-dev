package cc.unitmesh.devti.diff.view

import com.intellij.diff.chains.DiffRequestProducer
import com.intellij.diff.impl.DiffEditorViewer
import com.intellij.diff.requests.ContentDiffRequest
import com.intellij.diff.requests.DiffRequest
import com.intellij.diff.util.DiffUserDataKeysEx
import com.intellij.openapi.ListSelection
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolder
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vcs.changes.ChangeViewDiffRequestProcessor
import com.intellij.openapi.vcs.changes.EditorTabDiffPreview
import com.intellij.openapi.vcs.changes.actions.diff.ChangeDiffRequestProducer
import com.intellij.openapi.vcs.changes.actions.diff.lst.LocalChangeListDiffRequest
import com.intellij.openapi.vcs.changes.actions.diff.lst.LocalChangeListDiffTool
import com.intellij.openapi.vcs.impl.PartialChangesUtil
import java.util.stream.Stream

class StandaloneEditorTabDiffPreview(
    project: Project,
    val change: Change,
    val enableStaging: Boolean,
    val logger: Logger
) : EditorTabDiffPreview(project) {
    override fun hasContent(): Boolean = true

    override fun collectDiffProducers(selectedOnly: Boolean): ListSelection<out DiffRequestProducer>? {
        val producer = ChangeDiffRequestProducer.create(project, this.change)
        if (producer != null) {
            return ListSelection.createSingleton<ChangeDiffRequestProducer>(producer)
        }
        return ListSelection.empty()
    }

    override fun createViewer(): DiffEditorViewer {
        return StandaloneDiffRequestProcessor(project, this.change, this.enableStaging, this.logger)
    }

    override fun getEditorTabName(processor: DiffEditorViewer?): String {
        val path = change.beforeRevision?.file?.path
            ?: change.afterRevision?.file?.path

        val fileName = path?.substringAfterLast('/') ?: "Unknown"
        return "Review Code Changes - $fileName"
    }
}

class StandaloneDiffRequestProcessor(
    project: Project,
    private val change: Change,
    private val enableStaging: Boolean,
    private val logger: Logger
) : ChangeViewDiffRequestProcessor(project, "StandaloneDiffPreview") {

    private val changeWrapper: ChangeWrapper = ChangeWrapper(this.change)

    init {
        putContextUserData(DiffUserDataKeysEx.LAST_REVISION_WITH_LOCAL, true)
        if (enableStaging) {
            logger.info("Enabling chunk staging in diff viewer")
            putContextUserData(LocalChangeListDiffTool.ALLOW_EXCLUDE_FROM_COMMIT, true)
        } else {
            logger.info("Chunk staging disabled by parameter")
        }
    }

    override fun getCurrentRequestProvider(): DiffRequestProducer? {
        return if (enableStaging && hasLineStatusTracker(change)) {
            createLocalChangeListDiffProducer(change)
        } else {
            ChangeDiffRequestProducer.create(project, change)
        }
    }

    override fun collectDiffProducers(selectedOnly: Boolean): ListSelection<out DiffRequestProducer> {
        return ChangeDiffRequestProducer.create(project, change)?.let { producer ->
            ListSelection.createSingleton(producer)
        } ?: ListSelection.empty()
    }

    override fun selectChange(change: Wrapper) {
        // This method is intentionally left empty.
    }

    @Deprecated("Deprecated in Java")
    override fun getAllChanges(): Stream<Wrapper> {
        return Stream.of(changeWrapper)
    }

    @Deprecated("Deprecated in Java")
    override fun getSelectedChanges(): Stream<Wrapper> {
        return Stream.of(changeWrapper)
    }

    private fun hasLineStatusTracker(change: Change): Boolean {
        return PartialChangesUtil.getPartialTracker(project, change) != null
    }

    private fun createLocalChangeListDiffProducer(change: Change): DiffRequestProducer? {
        val file = change.virtualFile ?: return null
        val changeList = ChangeListManager.getInstance(project).getChangeList(change) ?: return null

        val changelistId = changeList.id
        val changelistName = changeList.name

        return object : DiffRequestProducer {
            override fun getName(): String {
                return ChangeDiffRequestProducer.getRequestTitle(change)
            }

            override fun process(context: UserDataHolder, indicator: ProgressIndicator): DiffRequest {
                val regularRequest = ChangeDiffRequestProducer.create(project, change)
                    ?.process(context, indicator) as? ContentDiffRequest

                if (regularRequest == null) {
                    val errorMessage = "Failed to create base diff request for change: $change"
                    logger.warn(errorMessage)
                    throw IllegalStateException(errorMessage)
                }

                return LocalChangeListDiffRequest(project, file, changelistId, changelistName, regularRequest)
            }
        }
    }
}