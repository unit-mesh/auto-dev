package cc.unitmesh.devti.intentions.error

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager

fun ProjectFileIndex.isInProject(virtualFile: VirtualFile, project: Project): Boolean {
    // https://github.com/JetBrains/intellij-community/blob/master/platform/projectModel-impl/src/com/intellij/openapi/roots/impl/ProjectFileIndexImpl.java#L32
    // new version has better method
    if (virtualFile.path.startsWith(project.basePath ?: return false)) {
        return true
    }

    PsiManager.getInstance(project).findFile(virtualFile)?.let {
        return true
    }

    return false
}