package cc.unitmesh.idea.provider

import cc.unitmesh.devti.provider.FrameworkConfigProvider
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope

class SpringFrameworkConfigProvider : FrameworkConfigProvider {
    private val files: Array<String> = arrayOf<String>(
        SpringBootConfigFileConstants.APPLICATION_PROPERTIES,
        SpringBootConfigFileConstants.BOOTSTRAP_PROPERTIES,
        SpringBootConfigFileConstants.ADDITIONAL_SPRING_CONFIGURATION_METADATA_JSON,
        SpringBootConfigFileConstants.APPLICATION_YML,
        "application.yaml",
        "devtools.properties",
        "devtools.yml",
        "devtools.yaml"
    )

    override suspend fun collect(project: Project): List<VirtualFile> {
        val modules = ModuleManager.getInstance(project).modules
        return modules.map { module ->
            val scope: GlobalSearchScope = module.getModuleScope(false)
            files.map { file ->
                runReadAction { FilenameIndex.getVirtualFilesByName(file, scope) }
            }.flatten()
        }.flatten()
    }
}

object SpringBootConfigFileConstants {
    const val APPLICATION: String = "application"
    const val APPLICATION_PROPERTIES: String = "application.properties"
    const val APPLICATION_YML: String = "application.yml"
    const val BOOTSTRAP: String = "bootstrap"
    const val BOOTSTRAP_PROPERTIES: String = "bootstrap.properties"
    const val SPRING_CONFIGURATION_METADATA_JSON: String = "spring-configuration-metadata.json"
    const val ADDITIONAL_SPRING_CONFIGURATION_METADATA_JSON: String = "additional-spring-configuration-metadata.json"
}
