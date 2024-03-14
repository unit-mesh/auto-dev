package cc.unitmesh.devti.language.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
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
            val commits: List<GitCommit> = GitHistoryUtils.history(project, repository.root, branchName)
            commits.forEach {
                val element = LookupElementBuilder.create(it.fullMessage)
                    .withIcon(AllIcons.Vcs.Branch)
                    .withTypeText(it.id.toShortString(), true)

                result.addElement(element)
            }
        } catch (e: Exception) {
//                e.printStackTrace()
        }
    }
}
