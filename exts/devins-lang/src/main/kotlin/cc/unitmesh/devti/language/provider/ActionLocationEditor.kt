package cc.unitmesh.devti.language.provider

import cc.unitmesh.devti.language.ast.config.DevInActionLocation
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project

interface ActionLocationEditor {
    fun isApplicable(hole: DevInActionLocation): Boolean

    fun resolve(project: Project, hole: DevInActionLocation): Editor?

    companion object {
        private val EP_NAME: ExtensionPointName<ActionLocationEditor> =
            ExtensionPointName.create("cc.unitmesh.shireActionLocationEditor")

        fun provide(project: Project, location: DevInActionLocation? = null): Editor? {
            if (location == null) {
                return defaultEditor(project)
            }

            val locationEditors = EP_NAME.extensionList.filter {
                it.isApplicable(location)
            }


            if (locationEditors.isNotEmpty()) {
                return locationEditors.first().resolve(project, location)
            }

            val dataContext = DataManager.getInstance().dataContextFromFocus.result
            val contextEditor = dataContext?.getData(CommonDataKeys.EDITOR)

            if (contextEditor != null) {
                return contextEditor
            }

            val savedDataContext = VariableActionEventDataHolder.getData()?.dataContext
            val editor = savedDataContext?.getData(CommonDataKeys.EDITOR)
            if (editor != null) {
                return editor
            }

            return defaultEditor(project)
        }

        fun defaultEditor(project: Project) =
            FileEditorManager.getInstance(project).selectedTextEditor
    }
}
