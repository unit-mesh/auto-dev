package cc.unitmesh.devti.context.builder

import com.intellij.lang.Language
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

interface CodeModifier {
    fun isApplicable(language: Language): Boolean
    fun insertTestCode(sourceFile: VirtualFile, project: Project, code: String): Boolean
    fun insertMethod(sourceFile: VirtualFile, project: Project, code: String): Boolean
    fun insertClass(sourceFile: VirtualFile, project: Project, code: String): Boolean
}