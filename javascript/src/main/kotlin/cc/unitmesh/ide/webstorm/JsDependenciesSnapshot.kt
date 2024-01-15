package cc.unitmesh.ide.webstorm

import cc.unitmesh.devti.provider.context.ChatCreationContext
import com.intellij.javascript.nodejs.PackageJsonData
import com.intellij.javascript.nodejs.packageJson.PackageJsonFileManager
import com.intellij.lang.javascript.buildTools.npm.PackageJsonUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile

class JsDependenciesSnapshot(
    val packageJsonFiles: Set<VirtualFile>,
    val resolvedPackageJson: Boolean,
    val tsConfigs: Set<VirtualFile>,
    val packages: Map<String, PackageJsonData.PackageJsonDependencyEntry>
) {
    companion object {
        fun create(
            project: Project,
            creationContext: ChatCreationContext?
        ): JsDependenciesSnapshot {
            var packageJsonFiles = emptySet<VirtualFile>()
            var resolvedPackageJson = false

            val virtualFile = creationContext?.sourceFile?.virtualFile
            if (virtualFile != null) {
                val packageJson = PackageJsonUtil.findUpPackageJson(virtualFile)
                if (packageJson != null) {
                    packageJsonFiles = setOf(packageJson)
                    resolvedPackageJson = true
                }
            }

            if (packageJsonFiles.isEmpty()) {
                packageJsonFiles = PackageJsonFileManager.getInstance(project).validPackageJsonFiles
            }

            val tsConfigs = findTsConfigs(project, packageJsonFiles)
            val packages = enumerateAllPackages(packageJsonFiles)
            return JsDependenciesSnapshot(packageJsonFiles, resolvedPackageJson, tsConfigs, packages)
        }

        private fun enumerateAllPackages(set: Set<VirtualFile>): Map<String, PackageJsonData.PackageJsonDependencyEntry> {
            return set.asSequence()
                .map { PackageJsonData.getOrCreate(it) }
                .flatMap { it.allDependencyEntries.entries }
                .associateBy({ it.key }, { it.value })
        }

        private fun findTsConfigs(project: Project, set: Set<VirtualFile>): Set<VirtualFile> {
            val mapNotNull = set.asSequence().mapNotNull {
                it.parent?.findChild("tsconfig.json")
            }

            val rootConfig = project.guessProjectDir()?.findChild("tsconfig.json")

            return mapNotNull.plus(rootConfig).filterNotNull().toSet()
        }
    }
}
