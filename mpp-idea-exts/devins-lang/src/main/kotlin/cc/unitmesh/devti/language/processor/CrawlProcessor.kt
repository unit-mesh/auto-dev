package cc.unitmesh.devti.language.processor

import cc.unitmesh.devti.agent.tool.browse.Browse
import cc.unitmesh.devti.language.ast.action.PatternActionFuncDef
import cc.unitmesh.devti.language.ast.action.PatternProcessor
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking

object CrawlProcessor: PatternProcessor {
    override val type: PatternActionFuncDef = PatternActionFuncDef.CRAWL

    suspend fun doExecute(url: String): String? {
        /// todo: parse github README.md if it's a github repo
        return Browse.parse(url).body
    }

    fun execute(urls: Array<out String>): List<String> {
        val results = runBlocking {
            coroutineScope {
                urls.mapNotNull {
                    try {
                        doExecute(it)
                    } catch (e: Exception) {
                        null
                    }
                }
            }
        }

        return results
    }
}
