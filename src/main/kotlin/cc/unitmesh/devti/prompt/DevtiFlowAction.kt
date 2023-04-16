package cc.unitmesh.devti.prompt

import cc.unitmesh.devti.analysis.DtClass
import cc.unitmesh.devti.kanban.SimpleProjectInfo

interface DevtiFlowAction {
    fun fillStoryDetail(project: SimpleProjectInfo, story: String): String
    fun analysisEndpoint(storyDetail: String, classes: List<DtClass>): String
    fun needUpdateMethodForController(targetEndpoint: String, clazz: DtClass): String
}