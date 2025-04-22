package cc.unitmesh.devti.language.startup

import cc.unitmesh.devti.language.ast.HobbitHole
import cc.unitmesh.devti.language.psi.DevInFile
import cc.unitmesh.devti.util.workerThread
import com.intellij.concurrency.ConcurrentCollectionFactory
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.LocalFileSystem.WatchRequest
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileEvent
import com.intellij.openapi.vfs.VirtualFileListener
import com.intellij.openapi.vfs.newvfs.RefreshQueue
import com.intellij.openapi.vfs.newvfs.impl.VfsData
import com.intellij.psi.PsiManager
import kotlinx.coroutines.CoroutineScope
import java.nio.file.Paths
import java.util.*
import kotlin.io.path.pathString

/**
 * Load all shire files in the specified path and its sub paths,and watch their changes.
 *
 * The root path may be outside the project path,It is necessary to load the shire files
 * and directories into vfs to ensure that [allChildrenLoaded][VfsData.DirectoryData.myAllChildrenLoaded] is true,
 * otherwise related events cannot be generated in processEvents of [RefreshQueue].
 *
 * @author lk
 */
@Service(Service.Level.APP)
class GlobalShireFileChangesProvider {

    // The root path can be configured in the future
    private val homeShirePath: String? =
        runCatching { Paths.get(System.getProperty("user.home"), ".shire").pathString }.getOrNull()

    private val watchRoots = ConcurrentCollectionFactory.createConcurrentMap<String, WatchRequest?>()

    private val localFileSystem: LocalFileSystem = LocalFileSystem.getInstance()

    private val listener: VirtualFileListener = object : VirtualFileListener {
        override fun fileCreated(event: VirtualFileEvent) {
            if (event.file.isDirectory) {
                loadShireAction(PathManager.getAbsolutePath(event.file.path))
            }
        }

        override fun beforeFileDeletion(event: VirtualFileEvent) {
            if (event.file.isDirectory) {
                removeShireAction(PathManager.getAbsolutePath(event.file.path))
            }

        }
    }

    @Volatile
    var shireFileModifier: ShireFileModifier? = null

    private fun addRootToWatch(path: String) {
        if (watchRoots.isEmpty()) localFileSystem.addVirtualFileListener(listener)
        watchRoots.computeIfAbsent(path) {
            localFileSystem.addRootToWatch(it, false)
        }
    }

    private fun removeRootToWatch(path: String) {
        // Do not delete the root path
        if (homeShirePath == path) return
        watchRoots.remove(path)
        if (watchRoots.isEmpty()) localFileSystem.removeVirtualFileListener(listener)
    }

    private fun loadShireAction(path: String) {
        if (homeShirePath == null || !path.startsWith(homeShirePath)) return
        refreshShireAction(path, ::addRootToWatch)
    }

    private fun removeShireAction(path: String) {
        val removedRoots = mutableSetOf<String>()
        refreshShireAction(path, removedRoots::add)
        removedRoots.forEach(::removeRootToWatch)
    }

    private fun refreshShireAction(path: String, handle: (String) -> Unit) {
        val queue = LinkedList<VirtualFile>()
        val file = localFileSystem.findFileByPath(path) ?: return
        queue.push(file)

        while (!queue.isEmpty()) {
            val virtualFile = queue.pop()
            if (virtualFile.isDirectory) {
                handle(PathManager.getAbsolutePath(virtualFile.path))
                virtualFile.children.forEach {
                    queue.push(it)
                }
            } else {
                ShireUpdater.publisher.onUpdated(virtualFile)
            }
        }
    }

    fun startup(afterUpdater: (HobbitHole, DevInFile) -> Unit) {
        if (homeShirePath == null) {
            logger.warn("Unable to access the root directory of the global shire file configuration")
            return
        }
        var initial = false
        (shireFileModifier ?: synchronized(this) {
            shireFileModifier ?: ShireFileModifier(ShireFileModificationContext(
                GlobalShireActionService.getInstance(),
                afterUpdater,
                CoroutineScope(workerThread)
            ) {
                ReadAction.compute<DevInFile?, Throwable> {
                    it.run {
                        ProjectManager.getInstance().openProjects.firstNotNullOfOrNull {
                            PsiManager.getInstance(it).findFile(this) as? DevInFile
                        }
                    }
                }
            }).also {
                initial = true
                shireFileModifier = it
            }
        }).startup {
            watchRoots.keys.contains(PathManager.getAbsolutePath(it.parent.path))
        }

        if (initial) loadShireAction(homeShirePath)

    }

    companion object {

        private val logger = logger<GlobalShireFileChangesProvider>()

        fun getInstance(): GlobalShireFileChangesProvider =
            ApplicationManager.getApplication().getService(GlobalShireFileChangesProvider::class.java)
    }
}