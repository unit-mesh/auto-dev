package cc.unitmesh.devti.language.middleware.builtin

import com.intellij.diff.DiffContentFactoryEx
import com.intellij.diff.DiffDialogHints
import com.intellij.diff.DiffManager
import com.intellij.diff.chains.SimpleDiffRequestChain
import com.intellij.diff.chains.SimpleDiffRequestProducer
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import cc.unitmesh.devti.devins.post.PostProcessor
import cc.unitmesh.devti.devins.post.PostProcessorContext
import cc.unitmesh.devti.devins.post.PostProcessorType
import cc.unitmesh.devti.language.utils.findFile

class DiffProcessor : PostProcessor {
    override val processorName: String = PostProcessorType.Diff.handleName
    override val description: String =
        "`diff` will show the diff of two texts, default is current code and llm response"

    private val diffFactory = DiffContentFactoryEx.getInstanceEx()

    override fun isApplicable(context: PostProcessorContext): Boolean {
        return true
    }

    override fun execute(project: Project, context: PostProcessorContext, console: ConsoleView?, args: List<Any>): Any {
        if (args.size < 2) {
            console?.print("DiffProcessor: not enough arguments", ConsoleViewContentType.ERROR_OUTPUT)
            return ""
        }

        val firstArg = args[0].toString()
        val virtualFile = runReadAction { project.findFile(firstArg) } ?: let {
            console?.print("DiffProcessor: file not found", ConsoleViewContentType.ERROR_OUTPUT)
            return ""
        }

        val currentDocContent = diffFactory.create(project, virtualFile)
        val newDocContent = diffFactory.create(args[1].toString())

        val diffRequest =
            SimpleDiffRequest("Shire Diff Viewer", currentDocContent, newDocContent, "Current code", "AI generated")
        val producer = SimpleDiffRequestProducer.create(virtualFile.path) {
            diffRequest
        }

        val chain = SimpleDiffRequestChain.fromProducer(producer)
        runInEdt {
            DiffManager.getInstance().showDiff(project, chain, DiffDialogHints.FRAME)
        }

        return ""
    }
}
