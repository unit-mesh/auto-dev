package cc.unitmesh.ide.javascript.flow

import com.intellij.lang.javascript.JavaScriptFileType
import com.intellij.lang.javascript.TypeScriptJSXFileType
import com.intellij.lang.javascript.frameworks.react.ReactFrameworkIndexingHandler
import com.intellij.lang.javascript.refactoring.react.ReactFunctionToClassComponentHandler
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope

enum class RouterFile(val filename: String) {
    UMI(".umirc.ts"),
    NEXT("next.config.js"),
    VITE("vite.config.js"),
}

class ReactFlow(
    val project: Project,
    override var userTask: String,
    val editor: Editor
) : FrontendFlow {
    val pages: MutableList<VirtualFile> = mutableListOf()
    val components: MutableList<VirtualFile> = mutableListOf()

    init {
        val searchScope: GlobalSearchScope = ProjectScope.getContentScope(project)
        // todo: find .umirc.ts in root, find in modules
        val umirc = FileTypeIndex.getFiles(TypeScriptJSXFileType.INSTANCE, searchScope).firstOrNull {
            it.name == ".umirc.ts"
        }

        FileTypeIndex.getFiles(JavaScriptFileType.INSTANCE, searchScope).forEach {
            val path = it.canonicalFile?.path

            if (path != null && path.contains("pages")) {
                pages.add(it)
            }

            if (path != null && path.contains("components")) {
                components.add(it)
            }
        }

        ReactFrameworkIndexingHandler()
    }

    override fun getRoutes(): List<String> {
        TODO("Not yet implemented")
    }

    override fun getPages(): List<DsComponent> {
        TODO("Not yet implemented")
    }

    override fun getComponents(): List<DsComponent> {
        TODO("Not yet implemented")
    }

    override fun getDesignSystemComponents(): List<DsComponent> {
        TODO("Not yet implemented")
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
}