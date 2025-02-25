package cc.unitmesh.ide.javascript.bridge

import cc.unitmesh.devti.bridge.provider.UiComponentProvider
import cc.unitmesh.ide.javascript.JsDependenciesSnapshot
import com.intellij.javascript.nodejs.PackageJsonData
import com.intellij.javascript.nodejs.packageJson.PackageJsonFileManager
import com.intellij.openapi.project.Project

abstract class FrameworkComponentProvider(val frameworkName: String) : UiComponentProvider() {
    override fun isApplicable(project: Project): Boolean {
        val jsonFiles = PackageJsonFileManager.getInstance(project).validPackageJsonFiles
        if (jsonFiles.isEmpty()) {
            return false
        }

        val allPackages: Map<String, PackageJsonData.PackageJsonDependencyEntry> =
            JsDependenciesSnapshot.Companion.enumerateAllPackages(jsonFiles)
        return allPackages.containsKey(frameworkName.lowercase())
    }
}