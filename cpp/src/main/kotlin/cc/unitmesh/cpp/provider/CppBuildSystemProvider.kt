package cc.unitmesh.cpp.provider

import cc.unitmesh.devti.provider.BuildSystemProvider
import cc.unitmesh.devti.template.context.DockerfileContext
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspace

class CppBuildSystemProvider : BuildSystemProvider() {
    override fun collect(project: Project): DockerfileContext? {
        val config = CMakefileUtil.collectConfigFiles(project)
        if (config.isEmpty()) {
            return null
        }

        if (config.any { it.name == "CMakeLists.txt" }) {
            return buildCMakefileContext(config.filter { it.name == "CMakeLists.txt" }, project)
        }

        return null
    }

    val cmakeMinimumRequiredRegex = Regex("""\bcmake_minimum_required\s*\(\s*VERSION\s*(\d+(\.\d+)*)\s*\)""")
    private fun buildCMakefileContext(config: Collection<VirtualFile>, project: Project): DockerfileContext? {
        val buildToolName = "CMake"
        val buildToolVersion = config.firstOrNull()?.let {
            val content = it.inputStream.bufferedReader().readText()
            val version = cmakeMinimumRequiredRegex.find(content)?.groupValues?.get(1)
            version
        } ?: "unknown"

        val cMakeWorkspace = CMakeWorkspace.getInstance(project)
        if (cMakeWorkspace.isInitialized) {
            // todo :load version from cmakefile
        }

        return DockerfileContext(
            buildToolName = buildToolName,
            buildToolVersion = buildToolVersion,
            languageName = "C++",
            languageVersion = "unknown",
            taskString = config.firstOrNull()?.contentsToByteArray()?.let { String(it) } ?: ""
        )

    }
}
