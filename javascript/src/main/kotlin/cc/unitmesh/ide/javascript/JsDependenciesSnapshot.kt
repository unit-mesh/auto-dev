package cc.unitmesh.ide.javascript

import cc.unitmesh.ide.javascript.provider.MOST_POPULAR_PACKAGES
import com.intellij.javascript.nodejs.PackageJsonData
import com.intellij.javascript.nodejs.packageJson.PackageJsonFileManager
import com.intellij.lang.javascript.buildTools.npm.PackageJsonUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile

/**
 * Represents a snapshot of JavaScript dependencies in a Kotlin language project.
 *
 * This class provides information about the JavaScript dependencies in the project, including the package.json files,
 * whether the package.json files have been resolved, the tsconfig.json files, and the packages defined in the package.json files.
 *
 * @property packageJsonFiles The set of package.json files in the project.
 * @property resolvedPackageJson A flag indicating whether the package.json files have been resolved.
 * @property tsConfigs The set of tsconfig.json files in the project.
 * @property packages The map of package names to their corresponding PackageJsonDependencyEntry objects.
 */
class JsDependenciesSnapshot(
    val packageJsonFiles: Set<VirtualFile>,
    private val resolvedPackageJson: Boolean,
    private val tsConfigs: Set<VirtualFile>,
    val packages: Map<String, PackageJsonData.PackageJsonDependencyEntry>
) {
    fun mostPopularFrameworks(): List<String> {
        val dependencies = this.packages
            .asSequence()
            .filter { entry -> MOST_POPULAR_PACKAGES.contains(entry.key) && !entry.key.startsWith("@type") }
            .map { entry ->
                val dependency = entry.key
                val version = entry.value.parseVersion()
                if (version != null) "$dependency: $version" else dependency
            }
            .toList()
        return dependencies
    }

    fun language(): String {
        var language = "JavaScript"
        var languageVersion = "ES5"

        val packageJson = this.packages["typescript"]
        val tsVersion = packageJson?.parseVersion()
        if (tsVersion != null) {
            language = "TypeScript"
            languageVersion = tsVersion.rawVersion
        }

        return "$language: $languageVersion"
    }

    companion object {
        fun create(project: Project, psiFile: PsiFile?): JsDependenciesSnapshot {
            var packageJsonFiles = emptySet<VirtualFile>()
            var resolvedPackageJson = false

            val virtualFile = psiFile?.virtualFile
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
