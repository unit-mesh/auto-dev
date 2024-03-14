package cc.unitmesh.devti.language.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.util.ProcessingContext
import git4idea.GitCommit
import git4idea.history.GitHistoryUtils
import git4idea.repo.GitRepositoryManager


class RevisionReferenceLanguageProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val project: Project = parameters.editor.project ?: return
        val repository = GitRepositoryManager.getInstance(project).repositories.firstOrNull() ?: return
        val branchName = repository.currentBranchName

        try {
            object : Task.Backgroundable(project, "loading git message", false) {
                override fun run(indicator: ProgressIndicator) {
                    val commits: List<GitCommit> = GitHistoryUtils.history(project, repository.root, branchName)
                    commits.forEach {
                        val element = LookupElementBuilder.create(it.id.toShortString())
                            .withIcon(AllIcons.Vcs.Branch)
                            .withTypeText(it.fullMessage, true)

                        result.addElement(element)
                    }
                }
            }.queue()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
