package cc.unitmesh.devti.indexer

import cc.unitmesh.devti.AutoDevIcons
import cc.unitmesh.devti.indexer.provider.LangDictProvider
import cc.unitmesh.devti.indexer.usage.PromptEnhancer
import cc.unitmesh.devti.llms.LlmFactory
import cc.unitmesh.devti.settings.AutoDevSettingsState
import cc.unitmesh.devti.settings.coder.coderSetting
import cc.unitmesh.devti.settings.locale.LanguageChangedCallback.presentationText
import cc.unitmesh.devti.statusbar.AutoDevStatus
import cc.unitmesh.devti.statusbar.AutoDevStatusService
import cc.unitmesh.devti.template.GENIUS_CODE
import cc.unitmesh.devti.template.TemplateRender
import cc.unitmesh.devti.template.context.TemplateContext
import cc.unitmesh.devti.util.AutoDevCoroutineScope
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.LocalFileSystem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.launch
import kotlin.io.path.createDirectories
import kotlin.io.path.exists

class DomainDictGenerateAction : AnAction() {
    init {
        presentationText("indexer.generate.domain", templatePresentation)
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val presentation = event.presentation
        
        AutoDevCoroutineScope.scope(project).launch {
            val baseDir = project.coderSetting.state.teamPromptsDir
            val prompt = buildPrompt(project)

            try {
                updatePresentation(presentation, AutoDevIcons.LOADING, false)
                AutoDevStatusService.notifyApplication(AutoDevStatus.InProgress)
                val promptDir = project.guessProjectDir()!!.toNioPath().resolve(baseDir)
                if (!promptDir.exists()) {
                    promptDir.createDirectories()
                }

                logger<DomainDictGenerateAction>().debug("Prompt: $prompt")

                val file = promptDir.resolve("domain.csv").toFile()

                // Stream LLM response and write directly to file
                val stream: Flow<String> = LlmFactory.create(project).stream(prompt, "")
                val result = StringBuilder()

                stream.cancellable().collect { chunk ->
                    result.append(chunk)
                }

                file.writeText(cleanCsvOutput(result.toString()))

                // After streaming is complete, open the file in editor
                ApplicationManager.getApplication().invokeLater {
                    val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
                    if (virtualFile != null) {
                        FileEditorManager.getInstance(project).openFile(virtualFile, true)
                    } else {
                        logger<DomainDictGenerateAction>().warn("Failed to open domain.csv after generation")
                    }
                }

                AutoDevStatusService.notifyApplication(AutoDevStatus.Done)
            } catch (e: Exception) {
                AutoDevStatusService.notifyApplication(AutoDevStatus.Error)
                e.printStackTrace()
            } finally {
                updatePresentation(presentation, AutoDevIcons.AI_COPILOT, true)
            }
        }
    }

    private suspend fun buildPrompt(project: Project): String {
        val maxTokenLength = AutoDevSettingsState.maxTokenLength
        
        // Collect semantic names with Level 1 + Level 2 structure and weights
        val domainDict = LangDictProvider.allSemantic(project, maxTokenLength)
        
        // Use weighted list for better LLM attention
        val codeContext = domainDict.toWeightedList()
        
        val templateRender = TemplateRender(GENIUS_CODE)
        val template = templateRender.getTemplate("indexer.vm")

        // Log semantic extraction info with weight analysis
        val weightStats = domainDict.getWeightStatistics()
        logger<DomainDictGenerateAction>().info(
            "Domain Dictionary: ${domainDict.metadata["level1_count"]} files, " +
            "${domainDict.metadata["level2_count"]} classes/methods, " +
            "Total tokens: ${domainDict.getTotalTokens()}, " +
            "Weight: Avg=${String.format("%.2f", weightStats["averageWeight"])}, " +
            "Critical=${weightStats["criticalCount"]}, " +
            "High=${weightStats["highCount"]}"
        )

        if (codeContext.isEmpty()) {
            throw IllegalStateException("No code context found for domain dictionary generation")
        }

        // Calculate token usage for code context and determine if README should be included
        val codeTokens = domainDict.getTotalTokens()
        val reservedTokens = 1000  // Reserve tokens for prompt template and response
        val remainingTokenBudget = maxTokenLength - codeTokens - reservedTokens
        
        // Only include README if there's sufficient remaining token budget
        val readme = if (remainingTokenBudget > 500) {
            val fullReadme = PromptEnhancer.readmeFile(project)
            // Truncate README if it's too large
            if (fullReadme.length > remainingTokenBudget * 4) {  // Rough estimate: 4 chars per token
                fullReadme.take(remainingTokenBudget * 4)
            } else {
                fullReadme
            }
        } else {
            ""  // Skip README if token budget is too tight
        }

        logger<DomainDictGenerateAction>().info(
            "Token budget: code=$codeTokens, reserved=$reservedTokens, remaining=$remainingTokenBudget, " +
            "readme_included=${readme.isNotEmpty()}"
        )

        val context = DomainDictGenerateContext(codeContext, readme)
        val prompt = templateRender.renderTemplate(template, context)
        return prompt
    }

    private fun updatePresentation(presentation: Presentation, icon: javax.swing.Icon, enabled: Boolean) {
        presentation.icon = icon
        presentation.isEnabled = enabled
    }

    /**
     * Clean CSV output from LLM response by:
     * 1. Extracting only valid CSV lines (ignoring markdown tables, explanations)
     * 2. Removing markdown table separators (|---|---|)
     * 3. Keeping only lines with CSV format (comma-separated values)
     * 4. Removing any trailing explanations or markdown formatting
     */
    private fun cleanCsvOutput(rawOutput: String): String {
        val lines = rawOutput.lines()
        val csvLines = mutableListOf<String>()
        var foundCsvHeader = false
        
        for ((index, line) in lines.withIndex()) {
            val trimmed = line.trim()
            
            // Skip empty lines
            if (trimmed.isEmpty()) {
                // If we already found CSV data and hit empty line, likely end of CSV section
                if (foundCsvHeader && csvLines.isNotEmpty()) break
                continue
            }
            
            // Skip markdown table separators (|---|---|...)
            if (trimmed.startsWith("|") && trimmed.contains("---")) continue
            
            // Try to parse as CSV line
            val csvLine = parseCsvLine(trimmed)
            if (csvLine != null) {
                // Check if this looks like a valid CSV data row (at least 3 columns)
                val columns = csvLine.split(",")
                if (columns.size >= 3) {
                    csvLines.add(csvLine)
                    foundCsvHeader = true
                    continue
                }
            }
            
            // If we've found CSV data and now encounter a non-CSV line, we might have reached the end
            if (foundCsvHeader && csvLines.isNotEmpty()) {
                // But let's be lenient - continue scanning in case CSV appears later
                // Only break if we see clear explanation text patterns
                if (isExplanationText(trimmed)) {
                    // Look ahead to see if there's more CSV data
                    val remainingLines = lines.drop(index + 1).takeWhile { it.trim().isNotEmpty() }
                    if (remainingLines.any { parseCsvLine(it.trim()) != null }) {
                        // There's more CSV, continue
                        continue
                    } else {
                        // No more CSV found, stop
                        break
                    }
                }
            }
        }
        
        // Return cleaned CSV (or empty if no valid data found)
        return csvLines.joinToString("\n").takeIf { it.isNotEmpty() } ?: "中文,代码翻译,描述"
    }
    
    /**
     * Try to parse a line as CSV, handling markdown table format
     */
    private fun parseCsvLine(line: String): String? {
        val trimmed = line.trim()
        
        // If it's a markdown table row, convert to CSV
        if (trimmed.startsWith("|") && trimmed.endsWith("|") && !trimmed.contains("---")) {
            val csvLine = trimmed.trim('|').split("|").joinToString(",") { it.trim() }
            return csvLine
        }
        
        // If it's already CSV format (contains commas), return as-is
        if (trimmed.contains(",") && !trimmed.startsWith("|")) {
            return trimmed
        }
        
        return null
    }
    
    /**
     * Check if a line looks like explanation text (not CSV data)
     */
    private fun isExplanationText(line: String): Boolean {
        val lowerLine = line.lowercase()
        
        // Common explanation patterns
        val patterns = listOf(
            "以下", "content excluded", "excluded", "排除", "technical", "implementation",
            "说明", "解释", "备注", "notes", "csv", "返回", "return", "结果", "result",
            "完成", "done", "finished", "end", "结束", "没有", "none", "没有了"
        )
        
        return patterns.any { lowerLine.contains(it) }
    }
}

data class DomainDictGenerateContext(
    val code: String,
    val readme: String
) : TemplateContext
