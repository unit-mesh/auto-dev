package cc.unitmesh.devti.context.builder

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

interface CodeInserter {
    abstract fun insertTestCode(sourceFile: VirtualFile, project: Project, code: String): Boolean
    abstract fun insertMethod(sourceFile: VirtualFile, project: Project, code: String): Boolean
    abstract fun insertClass(sourceFile: VirtualFile, project: Project, code: String): Boolean
}