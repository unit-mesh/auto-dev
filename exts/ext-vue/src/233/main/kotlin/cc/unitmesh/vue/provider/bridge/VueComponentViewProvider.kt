package cc.unitmesh.vue.provider.bridge

import cc.unitmesh.devti.bridge.provider.ComponentViewProvider
import cc.unitmesh.devti.bridge.archview.model.UiComponent
import cc.unitmesh.devti.bridge.provider.ComponentViewMode
import cc.unitmesh.devti.util.relativePath
import com.intellij.javascript.nodejs.PackageJsonData
import com.intellij.javascript.nodejs.packageJson.PackageJsonFileManager
import com.intellij.javascript.web.html.WebFrameworkHtmlFileType
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.fileTypes.ex.FileTypeManagerEx
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
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
import java.util.concurrent.CompletableFuture

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

    override fun collect(project: Project, mode: ComponentViewMode): List<UiComponent> {
        val searchScope: GlobalSearchScope = ProjectScope.getContentScope(project)

        val fileType = FileTypeManagerEx.getInstanceEx().getFileTypeByExtension("vue")
                as? WebFrameworkHtmlFileType
            ?: return emptyList()

        val future = CompletableFuture<List<UiComponent>>()
        val task = object : Task.Backgroundable(project, "Processing context", false) {
            override fun run(indicator: ProgressIndicator) {
                runReadAction {
                    val virtualFiles = FileTypeIndex.getFiles(fileType, searchScope)
                    val components = mutableListOf<UiComponent>()

                    val psiManager = PsiManager.getInstance(project)
                    virtualFiles.forEach { file ->
                        val xmlFile = psiManager.findFile(file) ?: return@forEach
                        components += slowlyBuildComponents(xmlFile)
                    }

                    future.complete(components)
                }
            }
        }

        ProgressManager.getInstance()
            .runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))

        return future.get()
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
                componentName = path.parent.url.substringAfterLast("/")
            }
            if (!isPublicComponent(path)) return emptyList()

            return listOf(
                UiComponent(
                    component.defaultName ?: componentName,
                    path.relativePath(project = xmlFile.project),
                    "",
                    component.props.map(VueInputProperty::name),
                    component.methods.map { it.name },
                    component.slots.map { it.name }
                )
            )
        }
    }
}
