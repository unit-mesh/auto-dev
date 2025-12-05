package cc.unitmesh.devti.language.startup

import cc.unitmesh.devti.language.DevInFileType
import cc.unitmesh.devti.language.ast.HobbitHole
import cc.unitmesh.devti.language.psi.DevInFile
import cc.unitmesh.devti.util.AutoDevCoroutineScope
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.AsyncFileListener
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.events.VFileCopyEvent
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.psi.PsiManager
import com.intellij.testFramework.LightVirtualFile


/**
 * It is used after the project is started
 * and can use [shireFileModifier] to handle shire file change events.
 *
 * @author lk
 */
@Service(Service.Level.PROJECT)
class ShireFileChangesProvider(val project: Project) : Disposable {

    @Volatile
    var shireFileModifier: ShireFileModifier? = null

    fun startup(afterUpdater: (HobbitHole, DevInFile) -> Unit) {
        (shireFileModifier ?: synchronized(this) {
            shireFileModifier ?: ShireFileModifier(
                ShireFileModificationContext(
                    DynamicShireActionService.getInstance(project),
                    afterUpdater,
                    AutoDevCoroutineScope.scope(project)
                ) {
                    ReadAction.compute<DevInFile?, Throwable> {
                        PsiManager.getInstance(project).findFile(it) as? DevInFile
                    }
                }).also { shireFileModifier = it }
        }).startup {
            ReadAction.compute<Boolean, Throwable> { ProjectFileIndex.getInstance(project).isInProject(it) }
        }

    }

    fun onUpdated(file: DevInFile) {
        ShireUpdater.publisher.onUpdated(file.virtualFile)
    }

    companion object {
        fun getInstance(project: Project): ShireFileChangesProvider {
            return project.getService(ShireFileChangesProvider::class.java)
        }

    }

    override fun dispose() {
        shireFileModifier?.dispose()
    }
}

internal class ShireFileModificationListener : FileDocumentManagerListener, DocumentListener, ShireFileListener {
    fun onUpdated(document: Document) {
        FileDocumentManager.getInstance().getFile(document).let { onUpdated(it) }
    }

    override fun documentChanged(event: DocumentEvent) {
        onUpdated(event.document)
    }

    override fun bulkUpdateFinished(document: Document) {
        onUpdated(document)
    }

    override fun unsavedDocumentDropped(document: Document) {
        onUpdated(document)
    }

}

internal class AsyncShireFileListener : AsyncFileListener, ShireFileListener {
    override fun prepareChange(events: MutableList<out VFileEvent>): AsyncFileListener.ChangeApplier {
        val beforeChangedEvents = mutableListOf<VFileEvent>()
        val afterChangedEvents = mutableListOf<VFileEvent>()
        for (event in events) {
            when (event) {
                is VFileDeleteEvent -> {
                    beforeChangedEvents.add(event)
                }

                else -> {
                    afterChangedEvents.add(event) // Maybe the file type has been changed
                }
            }
        }

        return object : AsyncFileListener.ChangeApplier {
            override fun beforeVfsChange() {
                beforeChangedEvents.forEach { onUpdated(it.file) }
            }

            override fun afterVfsChange() {
                afterChangedEvents.forEach {
                    when (it) {
                        is VFileCopyEvent -> {
                            onUpdated(it.findCreatedFile())
                        }

                        else -> {
                            onUpdated(it.file)
                        }
                    }
                }
            }
        }

    }


}

/**
 * Only handle events related to shire file
 */
interface ShireFileListener {
    fun onUpdated(file: VirtualFile?) {
        try {
            if (file == null || !file.isValid || file.isDirectory) return
            if (file.fileType !is DevInFileType) return
            if (file is LightVirtualFile) return
            ShireUpdater.publisher.onUpdated(file)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}