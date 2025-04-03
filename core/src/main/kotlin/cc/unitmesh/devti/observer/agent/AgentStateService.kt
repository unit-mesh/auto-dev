package cc.unitmesh.devti.observer.agent

import cc.unitmesh.devti.agent.tool.AgentTool
import cc.unitmesh.devti.devin.dataprovider.BuiltinCommand
import cc.unitmesh.devti.llms.custom.Message
import cc.unitmesh.devti.llms.tokenizer.TokenizerFactory
import cc.unitmesh.devti.observer.plan.AgentTaskEntry
import cc.unitmesh.devti.observer.plan.MarkdownPlanParser
import cc.unitmesh.devti.observer.plan.PlanUpdateListener
import cc.unitmesh.devti.settings.AutoDevSettingsState
import cc.unitmesh.devti.util.parser.MarkdownCodeHelper
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diff.impl.patch.TextFilePatch
import com.intellij.openapi.diff.impl.patch.apply.GenericPatchApplier
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.openapi.vcs.FilePath
import com.intellij.openapi.vcs.FileStatus
import com.intellij.openapi.vcs.VcsBundle
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ContentRevision
import com.intellij.openapi.vcs.changes.CurrentContentRevision
import com.intellij.openapi.vcs.changes.TextRevisionNumber
import com.intellij.openapi.vcs.history.VcsRevisionNumber
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.vcsUtil.VcsUtil
import org.jetbrains.annotations.NonNls
import java.io.File
import java.io.IOException

@Service(Service.Level.PROJECT)
class AgentStateService(val project: Project) {
    val maxToken = AutoDevSettingsState.maxTokenLength
    var state: AgentState = AgentState()
    var tokenizer = lazy {
        TokenizerFactory.createTokenizer()
    }

    fun addTools(tools: List<BuiltinCommand>) {
        state.usedTools = tools.map {
            AgentTool(it.commandName, it.description, "")
        }

        logger<AgentStateService>().info("Called agent tools:\n ${state.usedTools.joinToString("\n")}")
    }

    fun addToChange(patch: TextFilePatch) {
        val baseDir = File(project.basePath!!)
        if (patch.afterName != null) {
            val newChangePath = getAbsolutePath(baseDir, patch.afterName).canonicalPath
            state.changes.removeIf {
                val afterRevision = it.afterRevision
                afterRevision != null && File(afterRevision.file.path).canonicalPath == newChangePath
            }
        }

        try {
            val change = createChange(patch)
            state.changes.add(change)

            ApplicationManager.getApplication().messageBus
                .syncPublisher(PlanUpdateListener.TOPIC)
                .onUpdateChange(state.changes)
        } catch (e: IOException) {
            logger<AgentStateService>().info(e)
        }
    }

    private fun createChange(patch: TextFilePatch): Change {
        val baseDir = File(project.basePath!!)
        val beforePath = patch.beforeName
        val afterPath = patch.afterName

        val fileStatus = if (patch.isNewFile) {
            FileStatus.ADDED
        } else if (patch.isDeletedFile) {
            FileStatus.DELETED
        } else {
            FileStatus.MODIFIED
        }

        val beforeFilePath = VcsUtil.getFilePath(getAbsolutePath(baseDir, beforePath), false)
        val afterFilePath = VcsUtil.getFilePath(getAbsolutePath(baseDir, afterPath), false)

        var beforeRevision: ContentRevision? = null
        if (fileStatus !== FileStatus.ADDED) {
            beforeRevision = object : CurrentContentRevision(beforeFilePath) {
                override fun getRevisionNumber(): VcsRevisionNumber {
                    return TextRevisionNumber(VcsBundle.message("local.version.title"))
                }
            }
        }

        var afterRevision: ContentRevision? = null
        if (fileStatus !== FileStatus.DELETED) {
            afterRevision = object : CurrentContentRevision(beforeFilePath) {
                override fun getRevisionNumber(): VcsRevisionNumber =
                    TextRevisionNumber(VcsBundle.message("local.version.title"))

                override fun getVirtualFile(): VirtualFile? = afterFilePath.virtualFile
                override fun getFile(): FilePath = afterFilePath
                override fun getContent(): @NonNls String? {
                    if (patch.isNewFile) {
                        return patch.singleHunkPatchText
                    }

                    if (patch.isDeletedFile) {
                        return null
                    }

                    val localContent: String = loadLocalContent()
                    val appliedPatch = GenericPatchApplier.apply(localContent, patch.hunks)
                    if (appliedPatch != null) {
                        return appliedPatch.patchedText
                    }
                    /// sometimes llm will return a wrong patch which the content is not correct
                    return patch.singleHunkPatchText
                }

                @Throws(VcsException::class)
                private fun loadLocalContent(): String {
                    return ReadAction.compute<String?, VcsException?>(ThrowableComputable {
                        val file: VirtualFile? = beforeFilePath.virtualFile
                        if (file == null) return@ThrowableComputable null
                        val doc = FileDocumentManager.getInstance().getDocument(file)
                        if (doc == null) return@ThrowableComputable null
                        doc.text
                    })
                }
            }
        }

        return Change(beforeRevision, afterRevision, fileStatus)
    }

    private fun getAbsolutePath(baseDir: File, relativePath: String): File {
        var file: File?
        try {
            file = File(baseDir, relativePath).getCanonicalFile()
        } catch (e: IOException) {
            logger<AgentStateService>().info(e)
            file = File(baseDir, relativePath)
        }

        return file
    }

    fun buildOriginIntention(): String? {
        val intention = state.messages
            .firstOrNull { it.role.lowercase() == "user" }
            ?.content

        if (intention != null) {
            state.originIntention = intention
        }

        return intention
    }

    fun getAllMessages(): List<Message> {
        return state.messages
    }

    /**
     * Call some LLM to compress it or use some other method to compress the history
     */
    fun processMessages(messages: List<Message>): List<Message> {
        val countLength = tokenizer.value.count(messages.joinToString("\n") { it.content })
        if (countLength < maxToken) {
            state.messages = messages
            return messages
        }

        state.messages = messages.map {
            it.copy(content = MarkdownCodeHelper.removeAllMarkdownCode(it.content))
        }

        return state.messages
    }

    fun updatePlan(items: MutableList<AgentTaskEntry>) {
        this.state.plan = items
        ApplicationManager.getApplication().messageBus
            .syncPublisher(PlanUpdateListener.TOPIC)
            .onPlanUpdate(items)
    }

    fun updatePlan(content: String) {
        val planItems = MarkdownPlanParser.parse(content)
        updatePlan(planItems.toMutableList())
    }

    fun resetState() {
        state = AgentState()
        val syncPublisher = ApplicationManager.getApplication().messageBus
            .syncPublisher(PlanUpdateListener.TOPIC)

        syncPublisher.onUpdateChange(mutableListOf())
        syncPublisher.onPlanUpdate(mutableListOf())
    }

    fun getPlan(): MutableList<AgentTaskEntry> {
        return state.plan
    }
}
