package cc.unitmesh.ide.javascript.flow

import cc.unitmesh.ide.javascript.flow.model.DsComponent
import cc.unitmesh.ide.javascript.util.ReactPsiUtil
import com.intellij.lang.javascript.JavaScriptFileType
import com.intellij.lang.javascript.TypeScriptJSXFileType
import com.intellij.lang.javascript.dialects.ECMA6LanguageDialect
import com.intellij.lang.javascript.dialects.TypeScriptJSXLanguageDialect
import com.intellij.lang.javascript.psi.JSFile
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
// keep this import
import kotlinx.serialization.decodeFromString
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
    private val pages: MutableList<DsComponent> = mutableListOf()
    private val components: MutableList<DsComponent> = mutableListOf()

    // config files
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
                path.contains("pages") -> buildComponent(jsFile)?.let {
                    pages += it
                }

                path.contains("components") -> buildComponent(jsFile)?.let {
                    components += it
                }

                else -> {
                    if (root.findChild(file.name) != null) {
                        RouterFile.values().filter { it.filename == file.name }.map {
                            routes += it to jsFile
                        }

                        configs.add(jsFile)
                    }
                }
            }
        }
    }

    override fun getPages(): List<DsComponent> = pages

    override fun getComponents(): List<DsComponent> = components

    private fun buildComponent(jsFile: JSFile): List<DsComponent>? {
        return when (jsFile.language) {
            is TypeScriptJSXLanguageDialect,
            is ECMA6LanguageDialect
            -> {
                val dsComponents = ReactPsiUtil.tsxComponentToComponent(jsFile)
                if (dsComponents.isEmpty()) {
                    logger<ReactAutoPage>().warn("no component found in ${jsFile.name}")
                }
                dsComponents
            }

            else -> {
                logger<ReactAutoPage>().warn("unknown language: ${jsFile.language}")
                null
            }
        }
    }

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
    override fun getDesignSystemComponents(): List<DsComponent> {
        val rootConfig = project.guessProjectDir()
            ?.findChild("prompts")
            ?.findChild("context")
            ?.findChild("ds.json") ?: return emptyList()

        val json = rootConfig.inputStream.reader().readText()
        return try {
            val result: List<DsComponent> = Json.decodeFromString(json)
            result
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun sampleRemoteCall(): String {
        TODO("Not yet implemented")
    }

    override fun sampleStateManagement(): String? {
        TODO("Not yet implemented")
    }

    override fun clarify(): String {
        TODO("Not yet implemented")
    }

    fun filterComponents(components: List<String>): List<DsComponent> {
        val comps = this.pages + this.components
        return components.mapNotNull { component ->
            comps.find { it.name == component }
        }
    }
}

