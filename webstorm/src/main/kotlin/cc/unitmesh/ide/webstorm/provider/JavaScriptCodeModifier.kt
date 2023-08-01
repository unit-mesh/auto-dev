package cc.unitmesh.ide.webstorm.provider

import cc.unitmesh.devti.context.builder.CodeModifier
import com.intellij.lang.Language
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class JavaScriptCodeModifier : CodeModifier {
    override fun isApplicable(language: Language): Boolean {
        TODO("Not yet implemented")
    }

    override fun insertTestCode(sourceFile: VirtualFile, project: Project, code: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun insertMethod(sourceFile: VirtualFile, project: Project, code: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun insertClass(sourceFile: VirtualFile, project: Project, code: String): Boolean {
        TODO("Not yet implemented")
    }

}