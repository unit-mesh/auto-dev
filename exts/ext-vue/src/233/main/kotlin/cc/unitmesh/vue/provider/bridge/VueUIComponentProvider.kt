package cc.unitmesh.vue.provider.bridge

import cc.unitmesh.devti.bridge.provider.UiComponentProvider
import cc.unitmesh.devti.bridge.tools.UiComponent
import com.intellij.lang.javascript.psi.JSExecutionScope
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.xml.XmlFile
import org.jetbrains.vuejs.index.findModule
import org.jetbrains.vuejs.index.findScriptTag
import org.jetbrains.vuejs.lang.html.VueFileType

class VueUIComponentProvider : UiComponentProvider() {
    override fun collect(project: Project): List<UiComponent> {
        val searchScope: GlobalSearchScope = ProjectScope.getContentScope(project)
        val psiManager = PsiManager.getInstance(project)

        val virtualFiles = FileTypeIndex.getFiles(VueFileType, searchScope)

        val components = mutableListOf<UiComponent>()
        virtualFiles.forEach { file ->
            val xmlFile = (psiManager.findFile(file) ?: return@forEach) as? XmlFile ?: return@forEach
            val scriptTag = findScriptTag(xmlFile, false) ?: return@forEach

            components += buildComponent(scriptTag as XmlFile) ?: return@forEach
        }

        return components
    }

    companion object {
        fun buildComponent(scriptTag: XmlFile): List<UiComponent> {
            val module: JSExecutionScope =
                findModule(scriptTag, false) ?: findModule(scriptTag, true) ?: return emptyList()

            return listOf(
                UiComponent(
                    module.name ?: "",
                    module.containingFile.virtualFile.url,
                    /// todo update from find module
                    module.containingFile.virtualFile.url
                )
            )
        }
    }
}
