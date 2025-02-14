package cc.unitmesh.git.actions.vcs

import cc.unitmesh.devti.provider.RevisionProvider
import cc.unitmesh.devti.vcs.VcsPrompting
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
import com.intellij.openapi.vcs.FilePath
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vcs.changes.CommitContext
import com.intellij.openapi.vcs.changes.LocalChangeList
import com.intellij.psi.PsiElement
import com.intellij.vcs.commit.ChangeListCommitState
import com.intellij.vcs.commit.LocalChangesCommitter
import com.intellij.vcsUtil.VcsUtil
import git4idea.GitCommit
import git4idea.GitRevisionNumber
import git4idea.changes.GitCommittedChangeListProvider
import git4idea.history.GitFileHistory
import git4idea.history.GitHistoryUtils
import git4idea.repo.GitRepositoryManager
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import java.util.concurrent.CompletableFuture


class GitRevisionProvider : RevisionProvider {
    private val logger = logger<GitRevisionProvider>()

    override fun fetchChanges(myProject: Project, revision: String): String? {
        val repository = GitRepositoryManager.getInstance(myProject).repositories.firstOrNull() ?: return null
        val future = CompletableFuture<List<Change>>()

        val task = object : Task.Backgroundable(myProject, "Loading", false) {
            override fun run(indicator: ProgressIndicator) {
                val committedChangeList = GitCommittedChangeListProvider.getCommittedChangeList(
                    myProject, repository.root, GitRevisionNumber(revision)
                )?.changes?.toList()

                future.complete(committedChangeList)
            }
        }

        ProgressManager.getInstance()
            .runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))


        return runBlocking {
            val changes = future.await()
            val diffContext = myProject.getService(VcsPrompting::class.java).prepareContext(changes)
            "\n```diff\n${diffContext}\n```\n"
        }
    }

    override fun fetchCompletions(project: Project, result: CompletionResultSet) {
        val repository = GitRepositoryManager.getInstance(project).repositories.firstOrNull() ?: return
        val branchName = repository.currentBranchName

        /**
         * Refs to [com.intellij.execution.process.OSProcessHandler.checkEdtAndReadAction], we should handle in this
         * way, another example can see in [git4idea.GitPushUtil.findOrPushRemoteBranch]
         */
        val future = CompletableFuture<List<GitCommit>>()
        val task = object : Task.Backgroundable(project, "Loading", false) {
            override fun run(indicator: ProgressIndicator) {
                val commits: List<GitCommit> = try {
                    // in some case, maybe not repo or branch, so we should handle it
                    GitHistoryUtils.history(project, repository.root, branchName)
                } catch (e: Exception) {
                    logger.error("Failed to fetch commits", e)
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

    override fun commitCode(project: Project, commitMessage: String): String {
        val changeListManager = ChangeListManager.getInstance(project)
        changeListManager.changeLists.forEach {
            val list: LocalChangeList = changeListManager.getChangeList(it.id) ?: return@forEach
            val commitState = ChangeListCommitState(list, list.changes.toList(), commitMessage)
            val committer = LocalChangesCommitter(project, commitState, CommitContext())
            committer.runCommit("Commit", false)
        }

        return "Committing..."
    }

    override fun countHistoryChange(project: Project, element: PsiElement): Int {
        val file = element.containingFile.virtualFile ?: return 0
        val filePath: FilePath = VcsUtil.getFilePath(file)

        return try {
            GitFileHistory.collectHistory(project, filePath).size
        } catch (e: Exception) {
            logger.error("Failed to count history changes for file: ${file.path}", e)
            0
        }
    }
}
