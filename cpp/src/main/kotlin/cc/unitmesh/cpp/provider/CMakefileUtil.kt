package cc.unitmesh.cpp.provider

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.cmake.psi.CMakeFile

object CMakefileUtil {
    private val configFiles = listOf(
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

    /**
     * Returns the name of the project defined in the given CMake file.
     *
     * @param file the CMake file to retrieve the project name from
     * @return the name of the project defined in the CMake file, or an empty string if not found
     */
    fun getProjectName(file: CMakeFile): String {
        file.topLevelCommands.forEach {
            if (it.cMakeCommandName?.text == "project") {
                return it.cMakeCommandArguments?.cMakeArgumentList?.first()?.text ?: ""
            }

        }

        return file.project.name
    }
}