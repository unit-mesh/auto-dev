package cc.unitmesh.devti.context.builder

import com.intellij.lang.Language
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

interface CodeModifier {
    abstract fun isApplicable(language: Language): Boolean
    abstract fun insertTestCode(sourceFile: VirtualFile, project: Project, code: String): Boolean
    abstract fun insertMethod(sourceFile: VirtualFile, project: Project, code: String): Boolean
    abstract fun insertClass(sourceFile: VirtualFile, project: Project, code: String): Boolean
}