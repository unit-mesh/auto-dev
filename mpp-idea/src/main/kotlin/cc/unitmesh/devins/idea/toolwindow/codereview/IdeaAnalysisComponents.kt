package cc.unitmesh.devins.idea.toolwindow.codereview

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.unitmesh.agent.codereview.ModifiedCodeRange
import cc.unitmesh.agent.linter.LintFileResult
import cc.unitmesh.agent.linter.LintIssue
import cc.unitmesh.agent.linter.LintSeverity
import cc.unitmesh.devins.idea.renderer.sketch.IdeaSketchRenderer
import cc.unitmesh.devins.ui.compose.agent.codereview.AnalysisStage
import cc.unitmesh.devins.ui.compose.agent.codereview.CodeReviewState
import cc.unitmesh.devins.ui.compose.agent.codereview.DiffFileInfo
import cc.unitmesh.devins.ui.compose.theme.AutoDevColors
import com.intellij.openapi.Disposable
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.*

@Composable
internal fun IdeaAIAnalysisPanel(state: CodeReviewState, viewModel: IdeaCodeReviewViewModel, parentDisposable: Disposable, modifier: Modifier = Modifier) {
    val progress = state.aiProgress
    Column(modifier = modifier.background(JewelTheme.globalColors.panelBackground)) {
        IdeaAnalysisHeader(progress.stage, state.diffFiles.isNotEmpty(), { viewModel.startAnalysis() }, { viewModel.cancelAnalysis() })
        Divider(Orientation.Horizontal, modifier = Modifier.fillMaxWidth().height(1.dp))
        state.error?.let { Text(it, style = JewelTheme.defaultTextStyle.copy(color = AutoDevColors.Red.c400, fontSize = 12.sp), modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) }
        Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            if (progress.stage == AnalysisStage.IDLE && progress.lintResults.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Click 'Start Review' to analyze code changes with AI", style = JewelTheme.defaultTextStyle.copy(color = JewelTheme.globalColors.text.info, fontSize = 12.sp))
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (progress.lintResults.isNotEmpty() || progress.lintOutput.isNotEmpty()) {
                        item { IdeaLintAnalysisCard(progress.lintResults, progress.lintOutput, progress.stage == AnalysisStage.RUNNING_LINT, state.diffFiles, progress.modifiedCodeRanges) }
                    }
                    if (progress.analysisOutput.isNotEmpty()) {
                        item { IdeaAIAnalysisSection(progress.analysisOutput, progress.stage == AnalysisStage.ANALYZING_LINT, parentDisposable) }
                    }
                    if (progress.planOutput.isNotEmpty()) {
                        item { IdeaModificationPlanSection(progress.planOutput, progress.stage == AnalysisStage.GENERATING_PLAN, parentDisposable) }
                    }
                    if (progress.stage == AnalysisStage.WAITING_FOR_USER_INPUT) {
                        item { IdeaUserInputSection({ viewModel.proceedToGenerateFixes(it) }, { viewModel.cancelAnalysis() }) }
                    }
                    if (progress.fixRenderer != null || progress.stage == AnalysisStage.GENERATING_FIX) {
                        item { IdeaSuggestedFixesSection(progress.fixOutput, progress.stage == AnalysisStage.GENERATING_FIX, parentDisposable) }
                    }
                }
            }
        }
    }
}

@Composable
internal fun IdeaAnalysisHeader(stage: AnalysisStage, hasDiffFiles: Boolean, onStartAnalysis: () -> Unit, onCancelAnalysis: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("AI Code Review", style = JewelTheme.defaultTextStyle.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp))
            val (statusText, statusColor) = when (stage) {
                AnalysisStage.IDLE -> "Ready" to JewelTheme.globalColors.text.info
                AnalysisStage.RUNNING_LINT -> "Linting..." to AutoDevColors.Amber.c400
                AnalysisStage.ANALYZING_LINT -> "Analyzing..." to AutoDevColors.Blue.c400
                AnalysisStage.GENERATING_PLAN -> "Planning..." to AutoDevColors.Blue.c400
                AnalysisStage.WAITING_FOR_USER_INPUT -> "Awaiting Input" to AutoDevColors.Amber.c400
                AnalysisStage.GENERATING_FIX -> "Fixing..." to AutoDevColors.Indigo.c400
                AnalysisStage.COMPLETED -> "Done" to AutoDevColors.Green.c400
                AnalysisStage.ERROR -> "Error" to AutoDevColors.Red.c400
            }
            if (stage != AnalysisStage.IDLE) {
                Box(modifier = Modifier.background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (stage != AnalysisStage.COMPLETED && stage != AnalysisStage.ERROR) CircularProgressIndicator()
                        Text(statusText, style = JewelTheme.defaultTextStyle.copy(color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Medium))
                    }
                }
            }
        }
        when (stage) {
            AnalysisStage.IDLE, AnalysisStage.COMPLETED, AnalysisStage.ERROR -> DefaultButton(onClick = onStartAnalysis, enabled = hasDiffFiles) { Text("Start Review") }
            else -> OutlinedButton(onClick = onCancelAnalysis) { Text("Cancel") }
        }
    }
}

@Composable
internal fun IdeaLintAnalysisCard(lintResults: List<LintFileResult>, lintOutput: String, isActive: Boolean, diffFiles: List<DiffFileInfo>, modifiedCodeRanges: Map<String, List<ModifiedCodeRange>>) {
    var isExpanded by remember { mutableStateOf(true) }
    val totalErrors = lintResults.sumOf { it.errorCount }
    val totalWarnings = lintResults.sumOf { it.warningCount }
    IdeaCollapsibleCard("Lint Analysis", isExpanded, { isExpanded = it }, isActive, {
        if (totalErrors > 0 || totalWarnings > 0) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (totalErrors > 0) IdeaBadge("$totalErrors errors", AutoDevColors.Red.c400)
                if (totalWarnings > 0) IdeaBadge("$totalWarnings warnings", AutoDevColors.Amber.c400)
            }
        }
    }) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            lintResults.forEach { result -> if (result.issues.isNotEmpty()) IdeaLintFileCard(result, modifiedCodeRanges[result.filePath] ?: emptyList()) }
            if (lintOutput.isNotEmpty() && lintResults.isEmpty()) Text(lintOutput, style = JewelTheme.defaultTextStyle.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp), modifier = Modifier.horizontalScroll(rememberScrollState()))
        }
    }
}

@Composable
private fun IdeaLintFileCard(fileResult: LintFileResult, modifiedRanges: List<ModifiedCodeRange>) {
    Column(modifier = Modifier.fillMaxWidth().background(JewelTheme.globalColors.panelBackground.copy(alpha = 0.5f), RoundedCornerShape(4.dp)).padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(fileResult.filePath.substringAfterLast("/"), style = JewelTheme.defaultTextStyle.copy(fontWeight = FontWeight.Medium, fontSize = 12.sp))
        fileResult.issues.take(5).forEach { IdeaLintIssueRow(it, modifiedRanges) }
        if (fileResult.issues.size > 5) Text("...and ${fileResult.issues.size - 5} more issues", style = JewelTheme.defaultTextStyle.copy(color = JewelTheme.globalColors.text.info, fontSize = 11.sp))
    }
}

@Composable
private fun IdeaLintIssueRow(issue: LintIssue, modifiedRanges: List<ModifiedCodeRange>) {
    val isInModifiedRange = modifiedRanges.any { issue.line in it.startLine..it.endLine }
    val severityColor = when (issue.severity) { LintSeverity.ERROR -> AutoDevColors.Red.c400; LintSeverity.WARNING -> AutoDevColors.Amber.c400; LintSeverity.INFO -> AutoDevColors.Blue.c400 }
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
        Text("L${issue.line}", style = JewelTheme.defaultTextStyle.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = if (isInModifiedRange) severityColor else JewelTheme.globalColors.text.info), modifier = Modifier.width(40.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(issue.message, style = JewelTheme.defaultTextStyle.copy(fontSize = 11.sp))
            issue.rule?.let { Text(it, style = JewelTheme.defaultTextStyle.copy(fontSize = 10.sp, color = JewelTheme.globalColors.text.info)) }
        }
    }
}

@Composable
internal fun IdeaAIAnalysisSection(analysisOutput: String, isActive: Boolean, parentDisposable: Disposable) {
    var isExpanded by remember { mutableStateOf(true) }
    IdeaCollapsibleCard("AI Analysis", isExpanded, { isExpanded = it }, isActive) {
        IdeaSketchRenderer.RenderResponse(analysisOutput, !isActive, parentDisposable, Modifier.fillMaxWidth())
    }
}

@Composable
internal fun IdeaModificationPlanSection(planOutput: String, isActive: Boolean, parentDisposable: Disposable) {
    var isExpanded by remember { mutableStateOf(true) }
    IdeaCollapsibleCard("Modification Plan", isExpanded, { isExpanded = it }, isActive, { if (isActive) IdeaBadge("Generating...", AutoDevColors.Blue.c400) }) {
        IdeaSketchRenderer.RenderResponse(planOutput, !isActive, parentDisposable, Modifier.fillMaxWidth())
    }
}

@Composable
internal fun IdeaUserInputSection(onGenerate: (String) -> Unit, onCancel: () -> Unit) {
    var userInput by remember { mutableStateOf(TextFieldValue("")) }
    IdeaCollapsibleCard("Your Feedback", true, {}, true, { IdeaBadge("Action Required", AutoDevColors.Amber.c400) }) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Review the plan above and provide any additional instructions:", style = JewelTheme.defaultTextStyle.copy(fontSize = 12.sp))
            TextArea(value = userInput, onValueChange = { userInput = it }, modifier = Modifier.fillMaxWidth().height(80.dp), placeholder = { Text("Optional: Add specific instructions or constraints...") })
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                OutlinedButton(onClick = onCancel) { Text("Cancel") }
                DefaultButton(onClick = { onGenerate(userInput.text) }) { Text("Generate Fixes") }
            }
        }
    }
}

@Composable
internal fun IdeaSuggestedFixesSection(fixOutput: String, isGenerating: Boolean, parentDisposable: Disposable) {
    var isExpanded by remember { mutableStateOf(true) }
    IdeaCollapsibleCard("Fix Generation", isExpanded, { isExpanded = it }, isGenerating, {
        if (isGenerating) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator()
                IdeaBadge("Generating...", AutoDevColors.Indigo.c400)
            }
        } else if (fixOutput.isNotEmpty()) IdeaBadge("Complete", AutoDevColors.Green.c400)
    }) {
        when {
            fixOutput.isNotEmpty() -> IdeaSketchRenderer.RenderResponse(fixOutput, !isGenerating, parentDisposable, Modifier.fillMaxWidth())
            isGenerating -> Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            else -> Text("No fixes generated yet.", style = JewelTheme.defaultTextStyle.copy(color = JewelTheme.globalColors.text.info, fontSize = 12.sp))
        }
    }
}

@Composable
internal fun IdeaCollapsibleCard(title: String, isExpanded: Boolean, onExpandChange: (Boolean) -> Unit, isActive: Boolean = false, badge: @Composable (() -> Unit)? = null, content: @Composable () -> Unit) {
    val backgroundColor = if (isActive) AutoDevColors.Blue.c600.copy(alpha = 0.08f) else JewelTheme.globalColors.panelBackground
    Column(modifier = Modifier.fillMaxWidth().background(backgroundColor, RoundedCornerShape(6.dp))) {
        Row(modifier = Modifier.fillMaxWidth().clickable { onExpandChange(!isExpanded) }.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (isExpanded) "-" else "+", style = JewelTheme.defaultTextStyle.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp))
                Text(title, style = JewelTheme.defaultTextStyle.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp))
                badge?.invoke()
            }
        }
        AnimatedVisibility(visible = isExpanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Box(modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) { content() }
        }
    }
}

@Composable
internal fun IdeaBadge(text: String, color: Color) {
    Box(modifier = Modifier.background(color.copy(alpha = 0.15f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
        Text(text, style = JewelTheme.defaultTextStyle.copy(color = color, fontSize = 10.sp, fontWeight = FontWeight.Medium))
    }
}
