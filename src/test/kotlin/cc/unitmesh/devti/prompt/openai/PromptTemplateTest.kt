package cc.unitmesh.devti.prompt.openai

import cc.unitmesh.devti.context.model.DtClass
import cc.unitmesh.devti.flow.PromptTemplate
import cc.unitmesh.devti.flow.model.SimpleProjectInfo
import org.junit.Assert.assertEquals
import org.junit.Ignore
import org.junit.Test

class PromptTemplateTest {

    @Test
    fun should_fill_story_detail() {
        val promptTemplate = PromptTemplate()
        val projectInfo = SimpleProjectInfo("", "AutoDev", "description")
        val story = "story information"

        val result = promptTemplate.storyDetail(projectInfo, story)
        assertEquals(
            """你是一个敏捷项目的 BA，请根据如下的信息，编写用户故事。

- 你的项目是：### AutoDev:description ###
- 你的需求是： ### story information ###。
- 必须要考虑、尽可能考虑各种异常场景，添加更多的 AC（至少 3 个）。
- 你的返回模板如下所示：

###
用户故事：可以选择宝贝出行服务
作为 xxx
我想 在xx出行的手机客户端里选择宝贝出行服务
以便于 我能够带宝宝打车出行的时候打到有儿童座椅的车

AC 1:  xxx
假设 xxx
当 xxx
于是 xxx
###
""", result
        )
    }

    @Test
    @Ignore
    fun should_fill_end_point_prompt() {
        val promptTemplate = PromptTemplate()
        val storyDetail = "用户故事：可以选择宝贝出行服务"
        val files: List<DtClass> = listOf(DtClass("TaxiController", emptyList()), DtClass("GpsController", emptyList()))
        val result = promptTemplate.createEndpoint(storyDetail, files)
        assertEquals(
            """你是一个资深的后端 CRUD 工程师，请根据下面的用户故事 和 Controller 列表。要求：

- 返回最合适的 Controller 名字
- 如果不存在合适的 Controller 名字，请返回适合的 Controller 名字。

Controller 列表：

###
TaxiController,GpsController
###

###
用户故事：可以选择宝贝出行服务
###
""", result
        )
    }
}