package cc.unitmesh.ide.javascript.flow

import com.intellij.lang.javascript.JavaScriptFileType
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope

class ReactFlow(
    val project: Project,
    override var userTask: String,
    val editor: Editor
) : FrontendFlow {
    init {
        val searchScope: GlobalSearchScope = ProjectScope.getContentScope(project)
        val jsFiles = FileTypeIndex.getFiles(JavaScriptFileType.INSTANCE, searchScope)

        println(jsFiles)
    }

    override fun isApplicable(): Boolean {
        return true
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