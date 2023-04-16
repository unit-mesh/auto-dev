package cc.unitmesh.devti.prompt

interface DevtiFlowAction {
    fun fillStoryDetail(story: String): String
}