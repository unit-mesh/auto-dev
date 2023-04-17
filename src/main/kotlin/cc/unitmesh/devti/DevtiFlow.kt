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

data class TargetEndpoint(
    val endpoint: String,
    var controller: DtClass,
    val hasMatchedController: Boolean = true
)

class DevtiFlow(
    private val kanban: Kanban,
    private val flowAction: DevtiFlowAction,
    private val processor: CrudProcessor? = null
) {
//    fun processAll(id: String) {
//        val storyDetail = fillStoryDetail(id)
//
//        val target = fetchSuggestEndpoint(storyDetail)
//        if (target == null) {
//            logger.warn("no suggest endpoint found")
//            return
//        }
//
//        updateEndpointMethod(target, storyDetail)
//    }

    /**
     * Step 3: update endpoint method
     */
    fun updateEndpointMethod(target: TargetEndpoint, storyDetail: String) {
        try {
            val code = fetchCode(target.endpoint, target.controller, storyDetail)
            processor?.createControllerOrUpdateMethod(target.controller.name, code, target.hasMatchedController)
        } catch (e: Exception) {
            logger.warn("update method failed: $e, try to fill update method 2nd")

            val code = fetchCode(target.endpoint, target.controller, storyDetail)
            processor?.createControllerOrUpdateMethod(target.controller.name, code, target.hasMatchedController)
        }
    }

    /**
     * Step 2: fetch suggest endpoint, if not found, return null
     */
    fun fetchSuggestEndpoint(storyDetail: String): TargetEndpoint {
        val files: List<DtClass> = processor?.controllerList() ?: emptyList()
        logger.warn("start devti flow")
        val targetEndpoint = flowAction.analysisEndpoint(storyDetail, files)
        // use regex match *Controller from targetEndpoint
        val controller = getController(targetEndpoint)
        if (controller == null) {
            logger.warn("no controller found from: $targetEndpoint")
            return TargetEndpoint(targetEndpoint, DtClass(targetEndpoint, listOf()), false)
        }

        logger.warn("target endpoint: $targetEndpoint")
        val targetController = files.find { it.name == targetEndpoint }
        if (targetController == null) {
            logger.warn("no controller found from: $targetEndpoint")
            return TargetEndpoint(targetEndpoint, DtClass(targetEndpoint, listOf()), false)
        }

        return TargetEndpoint(targetEndpoint, targetController)
    }

    /**
     * Step 1: check story detail is valid, if not, fill story detail
     */
    fun fillStoryDetail(id: String): String {
        val simpleProject = kanban.getProjectInfo()
        val story = kanban.getStoryById(id)

        // 1. check story detail is valid, if not, fill story detail
        var storyDetail = story.description
        if (!kanban.isValidStory(storyDetail)) {
            logger.warn("story detail is not valid, fill story detail")

            storyDetail = flowAction.fillStoryDetail(simpleProject, story.description)

            val newStory = SimpleStory(story.id, story.title, storyDetail)
            kanban.updateStoryDetail(newStory)
        }
        logger.warn("user story detail: $storyDetail")
        return storyDetail
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

    companion object {
        private val logger: Logger = logger<DtRunState>()
        fun getController(targetEndpoint: String): String? {
            val regex = Regex("""(\w+Controller)""")
            val matchResult = regex.find(targetEndpoint)
            return matchResult?.groupValues?.get(1)
        }
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