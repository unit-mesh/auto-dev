package cc.unitmesh.vue.provider.bridge

import cc.unitmesh.devti.bridge.provider.UiComponentProvider
import cc.unitmesh.devti.bridge.tools.UiComponent
import cc.unitmesh.devti.util.relativePath
import com.intellij.javascript.web.html.WebFrameworkHtmlFileType
import com.intellij.openapi.fileTypes.ex.FileTypeManagerEx
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.xml.XmlFile
import org.jetbrains.vuejs.index.findScriptTag
import org.jetbrains.vuejs.model.VueInputProperty
import org.jetbrains.vuejs.model.VueModelManager
import org.jetbrains.vuejs.model.VueRegularComponent

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
            components += buildComponent(xmlFile)
        }

        return components
    }

    companion object {
        /**
         * It's very slow ly
         */
        fun buildComponent(xmlFile: XmlFile): List<UiComponent> {
            val component = VueModelManager.findEnclosingContainer(xmlFile) as? VueRegularComponent ?: return listOf()
            val path = xmlFile.virtualFile
            var componentName = path.name
            if (componentName == "index.vue") {
                componentName = path.parent.url
            }
            if (component.props.isEmpty()) {
                return emptyList()
            }

            return listOf(
                UiComponent(
                    component.element ?: componentName,
                    path.relativePath(project = xmlFile.project),
                    "",
                    component.props.map(VueInputProperty::name)
                )
            )
        }
    }
}
