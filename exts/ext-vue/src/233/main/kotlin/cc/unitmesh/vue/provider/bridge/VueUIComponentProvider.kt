package cc.unitmesh.vue.provider.bridge

import cc.unitmesh.devti.bridge.provider.UiComponentProvider
import cc.unitmesh.devti.bridge.tools.UiComponent
import cc.unitmesh.devti.util.relativePath
import com.intellij.javascript.web.html.WebFrameworkHtmlFileType
import com.intellij.lang.javascript.psi.JSExecutionScope
import com.intellij.openapi.fileTypes.ex.FileTypeManagerEx
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import org.jetbrains.vuejs.index.findModule
import org.jetbrains.vuejs.index.findScriptTag

class VueUIComponentProvider : UiComponentProvider() {
    override fun collect(project: Project): List<UiComponent> {
        val searchScope: GlobalSearchScope = ProjectScope.getContentScope(project)

        val fileType = FileTypeManagerEx.getInstanceEx().getFileTypeByExtension("vue")
                as? WebFrameworkHtmlFileType
            ?: return emptyList()

        val virtualFiles = FileTypeIndex.getFiles(fileType, searchScope)

        val components = mutableListOf<UiComponent>()
        val psiManager = PsiManager.getInstance(project)

        virtualFiles.forEach { file ->
            val xmlFile = psiManager.findFile(file) as? XmlFile ?: return@forEach
            val scriptTag = findScriptTag(xmlFile, false) ?: return@forEach
            components += buildComponent(scriptTag, xmlFile)
        }

        return components
    }

    companion object {
        fun buildComponent(scriptTag: XmlTag, xmlFile: XmlFile): List<UiComponent> {
            val module: JSExecutionScope =
                findModule(scriptTag, false) ?: findModule(scriptTag, true) ?: return emptyList()

            val path = module.containingFile.virtualFile
            var componentName = path.name
            if (componentName == "index.vue") {
                componentName = path.parent.url
            }

            return listOf(
                UiComponent(
                    componentName,
                    path.relativePath(project = xmlFile.project),
                    componentName
                )
            )
        }
    }
}
