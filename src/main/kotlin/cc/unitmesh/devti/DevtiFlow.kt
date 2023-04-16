package cc.unitmesh.devti

import cc.unitmesh.devti.analysis.CrudProcessor
import cc.unitmesh.devti.analysis.DtClass
import cc.unitmesh.devti.kanban.Kanban
import cc.unitmesh.devti.kanban.SimpleStory
import cc.unitmesh.devti.prompt.DevtiFlowAction
import cc.unitmesh.devti.runconfig.DtRunState
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import org.commonmark.node.*
import org.commonmark.parser.Parser


class DevtiFlow(
    private val kanban: Kanban,
    private val flowAction: DevtiFlowAction,
    private val analyser: CrudProcessor? = null
) {
    fun start(id: String) {
        val project = kanban.getProjectInfo()
        val story = kanban.getStoryById(id)

        // 1. check story detail is valid, if not, fill story detail
        var storyDetail = story.description
        if (!kanban.isValidStory(storyDetail)) {
            logger.warn("story detail is not valid, fill story detail")

            storyDetail = flowAction.fillStoryDetail(project, story.description)

            val newStory = SimpleStory(story.id, story.title, storyDetail)
            kanban.updateStoryDetail(newStory)
        }
        logger.warn("user story detail: $storyDetail")

        // 2. get suggest endpoint
        val files: List<DtClass> = analyser?.controllerList() ?: emptyList()
        logger.warn("start devti flow")
        val targetEndpoint = flowAction.analysisEndpoint(storyDetail, files)
        // use regex match *Controller from targetEndpoint
        val controller = getController(targetEndpoint)
        if (controller == null) {
            logger.warn("no controller found from: $targetEndpoint")
            return
        }

        logger.warn("target endpoint: $targetEndpoint")
        val targetController = files.find { it.name == targetEndpoint }
        if (targetController == null) {
            logger.warn("no controller found from: $targetEndpoint")
            return
        }

        // 3. update endpoint method
        val code = fetchCode(targetEndpoint, targetController, storyDetail)
        try {
            analyser?.updateMethod(targetController.name, code)
        } catch (e: Exception) {
            logger.warn("update method failed: $e")
            logger.warn("try to fill update method 2nd")

            val code = fetchCode(targetEndpoint, targetController, storyDetail)
            analyser?.updateMethod(targetController.name, code)
        }
    }

    private fun fetchCode(
        targetEndpoint: String,
        targetController: DtClass,
        storyDetail: String
    ): String {
        val content = flowAction.needUpdateMethodForController(targetEndpoint, targetController, storyDetail)
        val code = parseCodeFromString(content)
        logger.warn("update method code: $code")
        return code
    }

    private fun parseCodeFromString(markdown: String): String {
        val parser: Parser = Parser.builder().build()
        val node: Node = parser.parse(markdown)
        val visitor = CodeVisitor()
        node.accept(visitor)
        return visitor.code
    }

    private fun getController(targetEndpoint: String): String? {
        val regex = Regex("""(\w+Controller)""")
        val matchResult = regex.find(targetEndpoint)
        return matchResult?.groupValues?.get(1)
    }

    companion object {
        private val logger: Logger = logger<DtRunState>()
    }
}


internal class CodeVisitor : AbstractVisitor() {
    var code = ""

    override fun visit(fencedCodeBlock: FencedCodeBlock?) {
        this.code = fencedCodeBlock?.literal ?: ""
    }

    override fun visit(indentedCodeBlock: IndentedCodeBlock?) {
        super.visit(indentedCodeBlock)
    }
}