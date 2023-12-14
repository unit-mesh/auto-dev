package cc.unitmesh.devti.flow.kanban

import cc.unitmesh.devti.flow.model.SimpleProjectInfo
import cc.unitmesh.devti.flow.model.SimpleStory

interface Kanban {
    fun isValidStory(content: String): Boolean {
        return content.contains("用户故事")
    }
    // 获取项目基本信息
    fun getProjectInfo(): SimpleProjectInfo
    // 不实现
    fun getStories(): List<SimpleStory>
    // 通过ID 获取用户故事，用户故事是存放在 issue 的commit里的
    fun getStoryById(storyId: String): SimpleStory
    // 提交commit
    fun updateStoryDetail(simpleStory: SimpleStory)
}
