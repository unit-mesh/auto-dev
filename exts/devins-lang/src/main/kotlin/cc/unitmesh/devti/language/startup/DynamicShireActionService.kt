package cc.unitmesh.devti.language.startup

import cc.unitmesh.devti.language.ast.config.ShireActionLocation
import cc.unitmesh.devti.language.psi.DevInFile
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.KeyboardShortcut
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.keymap.KeymapManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.util.*

@Service(Service.Level.PROJECT)
class DynamicShireActionService: DynamicActionService {
    private val dynamicActionService = GlobalShireActionService.getInstance()

    private val actionCache = WeakHashMap<DevInFile, DynamicDevInsActionConfig>()

    override fun putAction(key: DevInFile, action: DynamicDevInsActionConfig) {
        actionCache[key] = action
    }

    override fun removeAction(key: DevInFile) = actionCache.keys.removeIf {
        it == key
    }

    override fun getAllActions(): List<DynamicDevInsActionConfig> {
        return (actionCache.values.toList() + dynamicActionService.getAllActions())
            .distinctBy { it.devinFile.virtualFile }
    }

    fun getActions(location: ShireActionLocation): List<DynamicDevInsActionConfig> {
        return getAllActions().filter {
            it.hole?.actionLocation == location && it.hole.enabled
        }
    }

    /**
     * Sets a keymap shortcut for a specified action ID.
     *
     * This method takes in the action ID of the desired action and a keyboard string representing the shortcut keys to be set.
     * It retrieves the action manager and keymap manager instances, then adds the specified keyboard shortcut to the active keymap.
     *
     * @param action The ID of the action for which the shortcut is being set.
     * @param keyboardShortcut A string representing the keyboard shortcut keys (e.g. "ctrl shift A").
     */
    fun bindShortcutToAction(action: AnAction, keyboardShortcut: KeyboardShortcut) {
        val actionId = ActionManager.getInstance().getId(action) ?: return

        val activeKeymap = KeymapManager.getInstance().activeKeymap

        activeKeymap.removeAllActionShortcuts(actionId)
        activeKeymap.addShortcut(actionId, keyboardShortcut)
    }

    companion object {
        fun getInstance(project: Project): DynamicShireActionService =
            project.getService(DynamicShireActionService::class.java)
    }
}

@Service(Service.Level.APP)
class GlobalShireActionService: DynamicActionService {
    private val globalActionCache = WeakHashMap<VirtualFile, DynamicDevInsActionConfig>()

    override fun putAction(key: DevInFile, action: DynamicDevInsActionConfig) {
        globalActionCache[key.virtualFile] = action
    }

    override fun removeAction(key: DevInFile) = globalActionCache.keys.removeIf{ key.virtualFile == it }

    override fun getAllActions(): List<DynamicDevInsActionConfig> = globalActionCache.values.toList()

    companion object {
        fun getInstance(): GlobalShireActionService =
            ApplicationManager.getApplication().getService(GlobalShireActionService::class.java)
    }

}

interface DynamicActionService {

    fun putAction(key: DevInFile, action: DynamicDevInsActionConfig)

    fun removeAction(key: DevInFile): Boolean

    fun getAllActions(): List<DynamicDevInsActionConfig>

}