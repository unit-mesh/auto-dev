package cc.unitmesh.devti.language.startup

import cc.unitmesh.devti.language.ast.HobbitHole
import cc.unitmesh.devti.language.compiler.HobbitHoleParser
import cc.unitmesh.devti.language.psi.DevInFile
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.messages.MessageBusConnection
import com.intellij.util.messages.Topic
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

/**
 * This class is provided to ShireFileChangesProvider to dynamically adjust
 * the shire action config when the content of the shire file changes.
 *
 * It supports delayed processing([delayTime]) to avoid duplicate updates as much as possible.
 *
 * @author lk
 */
class ShireFileModifier(val context: ShireFileModificationContext) {
    private val dynamicActionService: DynamicActionService

    private val scope: CoroutineScope

    init {
        dynamicActionService = context.dynamicActionService
        scope = context.scope
    }

    private val queue: MutableSet<DevInFile> = mutableSetOf()

    private val waitingUpdateQueue: ArrayDeque<DevInFile> = ArrayDeque()

    private val delayTime: Long = TimeUnit.SECONDS.toMillis(3)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val dispatcher = Dispatchers.IO.limitedParallelism(1)

    @Volatile
    var connect: MessageBusConnection? = null

    private fun modify(afterUpdater: ((HobbitHole, DevInFile) -> Unit)?) {
        scope.launch(dispatcher) {
            delay(delayTime)
            synchronized(queue) {
                waitingUpdateQueue.addAll(queue)
                queue.clear()
            }
            runBlocking {
                runReadAction {
                    waitingUpdateQueue.forEach { file ->
                        if (!file.isValid) {
                            dynamicActionService.removeAction(file)
                            logger.debug("DevIns file[${file.name}] is deleted")
                            file.virtualFile.takeIf { it.isValid }?.run { context.convertor.invoke(this)?.let { println("reload.")
                                loadShireAction(it, afterUpdater) } }
                            return@forEach
                        }
                        if (!file.isPhysical) return@forEach
                        loadShireAction(file, afterUpdater)
                    }
                }
                waitingUpdateQueue.clear()
            }
        }
    }

    private fun loadShireAction(file: DevInFile, afterUpdater: ((HobbitHole, DevInFile) -> Unit)?) {
        try {
            HobbitHoleParser.parse(file).let {
                dynamicActionService.putAction(file, DynamicDevInsActionConfig(it?.name ?: file.name, it, file))
                if (it != null) afterUpdater?.invoke(it, file)
                logger.debug("DevIns file[${file.virtualFile.path}] is loaded")
            }
        } catch (e: Exception) {
            logger.error("An error occurred while parsing shire file: ${file.virtualFile.path}", e)
        }
    }

    fun startup(predicate: (VirtualFile) -> Boolean) {
        connect ?: synchronized(this) {
            connect ?: ShireUpdater.register { it.takeIf(predicate)?.let(context.convertor)?.let { add(it) } }.also { connect = it }
        }

    }

    private fun add(file: DevInFile) {
        synchronized(queue) {
            queue.add(file)
        }
        modify(context.afterUpdater)
    }

    fun dispose() {
        connect?.dispose()
    }

    companion object {

        private val logger = logger<ShireFileModifier>()

    }
}


fun interface ShireUpdater {
    fun onUpdated(file: VirtualFile)

    companion object {
        @Topic.ProjectLevel
        val TOPIC: Topic<ShireUpdater> = Topic.create("DevIns file updated", ShireUpdater::class.java)

        val publisher: ShireUpdater
            get() = ApplicationManager.getApplication().messageBus.syncPublisher(TOPIC)

        fun register(subscriber: ShireUpdater): MessageBusConnection {
            val connection = ApplicationManager.getApplication().messageBus.connect()
            connection.subscribe(TOPIC, subscriber)
            return connection
        }
    }
}

data class ShireFileModificationContext(
    val dynamicActionService: DynamicActionService,
    val afterUpdater: ((HobbitHole, DevInFile) -> Unit)?,
    val scope: CoroutineScope,
    val convertor: (VirtualFile) -> DevInFile?
)
