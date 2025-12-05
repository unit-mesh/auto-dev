package cc.unitmesh.ide.javascript.flow

import cc.unitmesh.devti.bridge.archview.model.UiComponent
import cc.unitmesh.ide.javascript.bridge.ReactComponentViewProvider
import com.intellij.lang.javascript.JavaScriptFileType
import com.intellij.lang.javascript.TypeScriptJSXFileType
import com.intellij.lang.javascript.psi.JSFile
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
// keep this import
import kotlinx.serialization.json.Json

enum class RouterFile(val filename: String) {
    UMI(".umirc.ts"),
    NEXT("next.config.js"),
    VITE("vite.config.js"),
}

class ReactAutoPage(
    private val project: Project,
    override var userTask: String,
    private val editor: Editor
) : AutoPage {
    // todo: add post routes design
    private val routes: MutableMap<RouterFile, JSFile> = mutableMapOf()
    private val pages: MutableList<UiComponent> = mutableListOf()
    private val components: MutableList<UiComponent> = mutableListOf()
    private val configs: MutableList<JSFile> = mutableListOf()

    init {
        val searchScope: GlobalSearchScope = ProjectScope.getContentScope(project)
        val psiManager = PsiManager.getInstance(project)

        val virtualFiles =
            FileTypeIndex.getFiles(JavaScriptFileType.INSTANCE, searchScope) +
                    FileTypeIndex.getFiles(TypeScriptJSXFileType.INSTANCE, searchScope)

        val root = project.guessProjectDir()!!

        virtualFiles.forEach { file ->
            val path = file.canonicalFile?.path ?: return@forEach

            val jsFile = (psiManager.findFile(file) ?: return@forEach) as? JSFile ?: return@forEach
            if (jsFile.isTestFile) return@forEach

            when {
                path.contains("views") -> ReactComponentViewProvider.buildComponent(jsFile)?.let {
                    pages += it
                }

                path.contains("pages") -> ReactComponentViewProvider.buildComponent(jsFile)?.let {
                    pages += it
                }

                path.contains("components") -> ReactComponentViewProvider.buildComponent(jsFile)?.let {
                    components += it
                }

                else -> {
                    if (root.findChild(file.name) != null) {
                        RouterFile.entries.filter { it.filename == file.name }.map {
                            routes += it to jsFile
                        }

                        configs.add(jsFile)
                    }
                }
            }
        }
    }

    override fun getPages(): List<UiComponent> = pages

    override fun getComponents(): List<UiComponent> = components

    override fun getRoutes(): Map<String, String> {
        return this.routes.map {
            when (it.key) {
                RouterFile.UMI -> emptyMap()
                RouterFile.NEXT -> {
                    pages.associate { page ->
                        val route = page.name.replace(Regex("([A-Z])"), "-$1").lowercase()
                        route to route
                    }
                }

                RouterFile.VITE -> emptyMap()
            }
        }.reduce { acc, map -> acc + map }
    }

    // load prompts/context/ds.json from project root
    override fun getDesignSystemComponents(): List<UiComponent> {
        val rootConfig = project.guessProjectDir()
            ?.findChild("prompts")
            ?.findChild("context")
            ?.findChild("ds.json") ?: return emptyList()

        val json = rootConfig.inputStream.reader().readText()
        return try {
            val result: List<UiComponent> = Json.decodeFromString(json)
            result
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun sampleRemoteCall(): String {
        // search for axios usage if exist package.json in code
        TODO("Not yet implemented")
    }

    override fun sampleStateManagement(): String? {
        // lookup for redux usage if exist package.json in code
        TODO("Not yet implemented")
    }

    override fun clarify(): String {
        TODO("Not yet implemented")
    }

    fun filterComponents(components: List<String>): List<UiComponent> {
        val comps = this.pages + this.components
        return components.mapNotNull { component ->
            comps.find { it.name == component }
        }
    }
}

