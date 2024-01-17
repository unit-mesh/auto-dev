package cc.unitmesh.cpp.provider

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile

object CMakefileUtil {
    val configFiles = listOf(
        "CMakeLists.txt", "meson.build", "Makefile", "ninja.build",
        "vcpkg.json", "BUILD", "sln", "vcxproj", "vcproj"
    )

    fun collectConfigFiles(project: Project): Collection<VirtualFile> =
        ProjectRootManager.getInstance(project).contentRoots
            .asSequence()
            .filter { it.isDirectory }
            .map { it.children }
            .map { file ->
                file.filter { configFiles.contains(it.name) || configFiles.contains(it.extension) }
            }
            .flatten()
            .toList()
}