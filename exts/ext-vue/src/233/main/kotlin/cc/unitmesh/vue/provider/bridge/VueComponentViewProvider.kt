package cc.unitmesh.vue.provider.bridge

import cc.unitmesh.devti.bridge.provider.ComponentViewProvider
import cc.unitmesh.devti.bridge.archview.model.UiComponent
import cc.unitmesh.devti.util.relativePath
import com.intellij.javascript.nodejs.PackageJsonData
import com.intellij.javascript.nodejs.packageJson.PackageJsonFileManager
import com.intellij.javascript.web.html.WebFrameworkHtmlFileType
import com.intellij.openapi.fileTypes.ex.FileTypeManagerEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import org.jetbrains.vuejs.model.VueInputProperty
import org.jetbrains.vuejs.model.VueModelManager
import org.jetbrains.vuejs.model.VueRegularComponent
import java.io.IOException

class VueComponentViewProvider : ComponentViewProvider() {
    override fun isApplicable(project: Project): Boolean {
        val jsonFiles = PackageJsonFileManager.getInstance(project).validPackageJsonFiles
        if (jsonFiles.isEmpty()) {
            return false
        }

        val allPackages: Map<String, PackageJsonData.PackageJsonDependencyEntry> =
            enumerateAllPackages(jsonFiles)
        return allPackages.containsKey("vue")
    }

    fun enumerateAllPackages(set: Set<VirtualFile>): Map<String, PackageJsonData.PackageJsonDependencyEntry> {
        return set.asSequence()
            .map { PackageJsonData.getOrCreate(it) }
            .flatMap { it.allDependencyEntries.entries }
            .associateBy({ it.key }, { it.value })
    }

    override fun collect(project: Project): List<UiComponent> {
        val searchScope: GlobalSearchScope = ProjectScope.getContentScope(project)

        val fileType = FileTypeManagerEx.getInstanceEx().getFileTypeByExtension("vue")
                as? WebFrameworkHtmlFileType
            ?: return emptyList()

        val virtualFiles = FileTypeIndex.getFiles(fileType, searchScope)

        val components = mutableListOf<UiComponent>()
        val psiManager = PsiManager.getInstance(project)

        virtualFiles.forEach { file ->
            val xmlFile = psiManager.findFile(file)  ?: return@forEach
            components += slowlyBuildComponents(xmlFile)
        }

        return components
    }

    companion object {
        fun isPublicComponent(file: VirtualFile): Boolean {
            return try {
                val content = VfsUtil.loadText(file)
                val propsRegex = Regex("props\\s*:\\s*[{\\[]")
                propsRegex.containsMatchIn(content)
            } catch (e: IOException) {
                false
            }
        }

        /**
         * It's very slow ly
         */
        fun slowlyBuildComponents(xmlFile: PsiFile): List<UiComponent> {
            val component = VueModelManager.findEnclosingContainer(xmlFile) as? VueRegularComponent ?: return listOf()
            val path = xmlFile.virtualFile
            var componentName = path.name
            if (componentName == "index.vue") {
                componentName = path.parent.url
            }
            if (!isPublicComponent(path)) {
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
