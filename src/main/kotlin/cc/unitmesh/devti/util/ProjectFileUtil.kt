// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cc.unitmesh.devti.util

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager

fun isInProject(virtualFile: VirtualFile, project: Project): Boolean {
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

fun Project.isInProject(virtualFile: VirtualFile): Boolean {
    return isInProject(virtualFile, this) || ProjectFileIndex.getInstance(this).isInLibrary(virtualFile)
}
