package cc.unitmesh.devti.observer.agent

import com.intellij.util.messages.Topic
import java.util.*

@FunctionalInterface
interface PlanUpdateListener: EventListener {
    fun onPlanUpdate(items: MutableList<PlanList>)

    companion object {
        @Topic.AppLevel
        val TOPIC: Topic<PlanUpdateListener> = Topic(
            PlanUpdateListener::class.java, Topic.BroadcastDirection.TO_DIRECT_CHILDREN
        )
    }
}