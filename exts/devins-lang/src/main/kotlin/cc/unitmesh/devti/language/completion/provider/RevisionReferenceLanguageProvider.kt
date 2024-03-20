package cc.unitmesh.devti.language.completion.provider

import cc.unitmesh.devti.language.DevInBundle
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.progress.runBlockingCancellable
import com.intellij.openapi.project.Project
import com.intellij.util.ProcessingContext
import git4idea.GitCommit
import git4idea.history.GitHistoryUtils
import git4idea.repo.GitRepositoryManager
import kotlinx.coroutines.future.await
import java.util.concurrent.CompletableFuture


class RevisionReferenceLanguageProvider : CompletionProvider<CompletionParameters>() {
    private val logger = logger<RevisionReferenceLanguageProvider>()

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val project: Project = parameters.editor.project ?: return
        val repository = GitRepositoryManager.getInstance(project).repositories.firstOrNull() ?: return
        val branchName = repository.currentBranchName

        /**
         * Refs to [com.intellij.execution.process.OSProcessHandler.checkEdtAndReadAction], we should handle in this
         * way, another example can see in [git4idea.GitPushUtil.findOrPushRemoteBranch]
         */
        val future = CompletableFuture<List<GitCommit>>()
        val task = object : Task.Backgroundable(project, DevInBundle.message("devin.ref.loading"), false) {
            override fun run(indicator: ProgressIndicator) {
                val commits: List<GitCommit> = try {
                    // in some case, maybe not repo or branch, so we should handle it
                    GitHistoryUtils.history(project, repository.root, branchName)
                } catch (e: Exception) {
                    logger.error("Failed to load commits", e)
                    emptyList()
                }

                future.complete(commits)
            }
        }

        ProgressManager.getInstance()
            .runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))

        runBlockingCancellable {
            val commits = future.await()
            commits.forEach {
                val element = LookupElementBuilder.create(it.id.toShortString())
                    .withIcon(AllIcons.Vcs.Branch)
                    .withPresentableText(it.fullMessage)
                    .withTypeText(it.id.toShortString(), true)

                result.addElement(element)
            }
        }
    }
}
