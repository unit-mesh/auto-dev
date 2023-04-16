package cc.unitmesh.devti.prompt

import cc.unitmesh.devti.kanban.SimpleProjectInfo

interface DevtiFlowAction {
    fun fillStoryDetail(project: SimpleProjectInfo, story: String): String
}