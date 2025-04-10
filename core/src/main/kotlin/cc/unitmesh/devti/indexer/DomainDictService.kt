package cc.unitmesh.devti.indexer

import cc.unitmesh.devti.mcp.host.readText
import cc.unitmesh.devti.settings.coder.coderSetting
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir

@Service(Service.Level.PROJECT)
class DomainDictService(val project: Project) {
    private val baseDir get() = project.coderSetting.state.teamPromptsDir
    private val basePromptDir get() = project.guessProjectDir()?.findChild(baseDir)

    fun domainDict(): List<List<String>> {
        val content = loadContent() ?: return emptyList()
        return csvReader().readAll(content)
    }

    fun loadContent(): String? {
        val promptsDir = basePromptDir ?: return null
        val dictFile = promptsDir.findChild("domain.csv") ?: return null
        val content = runReadAction { dictFile.readText() }
        return content
    }
}