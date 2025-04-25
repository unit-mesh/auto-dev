package cc.unitmesh.devti.language.middleware.builtin

import com.intellij.execution.ui.ConsoleView
import com.intellij.openapi.project.Project
import cc.unitmesh.devti.devins.post.PostProcessorType
import cc.unitmesh.devti.devins.post.PostProcessorContext
import cc.unitmesh.devti.devins.post.PostProcessor

class TimeMetricProcessor : PostProcessor {
    private var startTime: Long? = null

    override val processorName: String = PostProcessorType.TimeMetric.handleName
    override val description: String = "`timeMetric` will calculate the time metric"

    override fun isApplicable(context: PostProcessorContext): Boolean = true

    override fun setup(context: PostProcessorContext): String {
        startTime = System.currentTimeMillis()
        return startTime.toString()
    }

    override fun execute(project: Project, context: PostProcessorContext, console: ConsoleView?, args: List<Any>): String {
        val endTime = System.currentTimeMillis()
        return (endTime - startTime!!).toString()
    }
}
