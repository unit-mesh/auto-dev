package cc.unitmesh.git.provider

import cc.unitmesh.devti.language.ast.variable.ToolchainVariable
import cc.unitmesh.devti.language.provider.ToolchainVariableProvider
import cc.unitmesh.devti.devins.VariableActionEventDataHolder
import cc.unitmesh.devti.language.ast.variable.toolchain.VcsToolchainVariable
import cc.unitmesh.devti.vcs.VcsPrompting
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsDataKeys
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.CurrentContentRevision
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.vcs.commit.CommitWorkflowUi
import com.intellij.vcs.log.VcsFullCommitDetails
import com.intellij.vcs.log.VcsLogDataKeys
import com.intellij.vcs.log.VcsLogFilterCollection
import com.intellij.vcs.log.VcsLogProvider
import com.intellij.vcs.log.impl.VcsProjectLog
import com.intellij.vcs.log.visible.filters.VcsLogFilterObject
import java.awt.EventQueue.invokeAndWait


class GitToolchainVariableProvider : ToolchainVariableProvider {
    private val logger = logger<GitToolchainVariableProvider>()

    override fun isResolvable(variable: ToolchainVariable, psiElement: PsiElement?, project: Project): Boolean {
        return when (variable) {
            VcsToolchainVariable.CurrentChanges -> true
            VcsToolchainVariable.HistoryCommitMessages -> true
            VcsToolchainVariable.CurrentBranch -> true
            VcsToolchainVariable.Diff -> true
            else -> false
        }
    }

    override fun resolve(
        variable: ToolchainVariable,
        project: Project,
        editor: Editor,
        psiElement: PsiElement?,
    ): ToolchainVariable {
        when (variable) {
            VcsToolchainVariable.CurrentChanges -> {
                val commitWorkflowUi = getCommitWorkflowUi()
                if (commitWorkflowUi !is CommitWorkflowUi) {
                    logger.warn("Cannot get commit workflow UI, you may not be in a commit workflow.")
                    return variable
                }
                var changes: List<Change>? = null
                invokeAndWait {
                    changes = getDiff(commitWorkflowUi)
                }

                if (changes == null) {
                    logger.warn("Cannot get changes.")
                    return variable
                }

                val diffContext = project.getService(VcsPrompting::class.java).prepareContext(changes!!)

                if (diffContext.isEmpty() || diffContext == "\n") {
                    logger.warn("Diff context is empty or cannot get enough useful context.")
                    return variable
                }

                variable.value = diffContext
                return variable
            }

            VcsToolchainVariable.CurrentBranch -> {
                val logProviders = VcsProjectLog.getLogProviders(project)
                val entry = logProviders.entries.firstOrNull() ?: return variable

                val logProvider = entry.value
                val branch = logProvider.getCurrentBranch(entry.key) ?: return variable

                variable.value = branch
            }

            VcsToolchainVariable.HistoryCommitMessages -> {
                val exampleCommitMessages = getHistoryCommitMessages(project)
                if (exampleCommitMessages != null) {
                    variable.value = exampleCommitMessages
                }
            }

            VcsToolchainVariable.Diff -> {
                val dataContext = VariableActionEventDataHolder.getData()?.dataContext
                variable.value = analysisLog(dataContext, project)
            }
        }

        return variable
    }

    private fun analysisLog(dataContext: DataContext?, project: Project): String {
        if (dataContext == null) {
            return ""
        }


        val vcsLog = dataContext.getData(VcsLogDataKeys.VCS_LOG) ?: return ""

        val details: List<VcsFullCommitDetails> = vcsLog.selectedDetails.toList()
        val selectList = dataContext.getData(VcsDataKeys.SELECTED_CHANGES).orEmpty().toList()

        val vcsPrompting = project.getService(VcsPrompting::class.java)
        val fullChangeContent =
            vcsPrompting.buildDiffPrompt(details, selectList.toList(), project)

        return fullChangeContent ?: ""
    }

    /**
     * Finds example commit messages based on the project's VCS log, takes the first three commits.
     * If the no user or user has committed anything yet, the current branch name is used instead.
     *
     * @param project The project for which to find example commit messages.
     * @return A string containing example commit messages, or null if no example messages are found.
     */
    private fun getHistoryCommitMessages(project: Project): String? {
        val logProviders = VcsProjectLog.getLogProviders(project)
        val entry = logProviders.entries.firstOrNull() ?: return null

        val logProvider = entry.value
        val branch = logProvider.getCurrentBranch(entry.key) ?: return null
        val user = logProvider.getCurrentUser(entry.key)

        val logFilter = if (user != null) {
            VcsLogFilterObject.collection(VcsLogFilterObject.fromUser(user, setOf()))
        } else {
            VcsLogFilterObject.collection(VcsLogFilterObject.fromBranch(branch))
        }

        return collectExamples(logProvider, entry.key, logFilter)
    }

    /**
     * Collects examples from the VcsLogProvider based on the provided filter.
     *
     * @param logProvider The VcsLogProvider used to retrieve commit information.
     * @param root The root VirtualFile of the project.
     * @param filter The VcsLogFilterCollection used to filter the commits.
     * @return A string containing the collected examples, or null if no examples are found.
     */
    private fun collectExamples(
        logProvider: VcsLogProvider,
        root: VirtualFile,
        filter: VcsLogFilterCollection,
    ): String? {
        val commits = logProvider.getCommitsMatchingFilter(root, filter, 3)

        if (commits.isEmpty()) return null

        val builder = StringBuilder("")
        val commitIds = commits.map { it.id.asString() }

        logProvider.readMetadata(root, commitIds) {
            val shortMsg = it.fullMessage.split("\n").firstOrNull() ?: it.fullMessage
            builder.append(shortMsg).append("\n")
        }

        return builder.toString()
    }

    private fun getDiff(commitWorkflowUi: CommitWorkflowUi): List<Change>? {
        val changes = commitWorkflowUi.getIncludedChanges()
        val unversionedFiles = commitWorkflowUi.getIncludedUnversionedFiles()

        val changeList = unversionedFiles.map {
            Change(null, CurrentContentRevision(it))
        }

        if (changes.isNotEmpty() || changeList.isNotEmpty()) {
            return changes + changeList
        }

        return null
    }

}

fun getCommitWorkflowUi(): CommitWorkflowUi? {
    VariableActionEventDataHolder.getData()?.dataContext?.let {
        val commitWorkflowUi = it.getData(VcsDataKeys.COMMIT_WORKFLOW_UI)
        return commitWorkflowUi as CommitWorkflowUi?
    }

    val dataContext = DataManager.getInstance().dataContextFromFocus.result
    val commitWorkflowUi = dataContext?.getData(VcsDataKeys.COMMIT_WORKFLOW_UI)
    return commitWorkflowUi
}