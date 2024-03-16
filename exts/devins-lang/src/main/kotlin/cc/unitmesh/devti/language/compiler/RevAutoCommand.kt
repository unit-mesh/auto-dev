package cc.unitmesh.devti.language.compiler

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.vcs.VcsPrompting
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import git4idea.GitRevisionNumber
import git4idea.changes.GitCommittedChangeListProvider
import git4idea.repo.GitRepositoryManager
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import java.util.concurrent.CompletableFuture


class RevAutoCommand(private val myProject: Project, private val revision: String) : AutoCommand {
    override fun execute(): String? {
        val repository = GitRepositoryManager.getInstance(myProject).repositories.firstOrNull() ?: return null
        val future = CompletableFuture<List<Change>>()

        val task = object : Task.Backgroundable(myProject, AutoDevBundle.message("devin.ref.loading"), false) {
            override fun run(indicator: ProgressIndicator) {
                val committedChangeList = GitCommittedChangeListProvider.getCommittedChangeList(
                    myProject!!, repository.root, GitRevisionNumber(revision)
                )?.changes?.toList()

                future.complete(committedChangeList)
            }
        }

        ProgressManager.getInstance()
            .runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))


        return runBlocking {
            val changes = future.await()
            val diffContext = myProject.service<VcsPrompting>().prepareContext(changes)
            "\n```diff\n${diffContext}\n```\n"
        }
    }
}
