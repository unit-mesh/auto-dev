/*
 * Copyright 2022 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cc.unitmesh.server.command

import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

class InMemoryConsumer : StreamConsumer {
    private val lines: Queue<String> = ConcurrentLinkedQueue()
    override fun consumeLine(line: String) {
        try {
            lines.add(line)
            when {
                // try catch for catch error, like in: [https://dx.phodal.com/docs/factor/error-handling.html](https://dx.phodal.com/docs/factor/error-handling.html)
                line.contains("Error: Unable to access jarfile") -> {
                    lines.add("<ArchGuard Tips>: 'Error: Unable to access jarfile' => 下载 Scanner 可能出错，请尝试连接 VPN 下载。访问： https://archguard.org/faq 了解更多")
                }
                line.contains("Invalid or corrupt jarfile") -> {
                    lines.add("<ArchGuard Tips>: 'Invalid or corrupt jarfile' => jar 包不完整，请删除，并尝试连接 VPN 下载。访问： https://archguard.org/faq 了解更多")
                }
                line.contains("Fail to clone source with exitCode 128") -> {
                    lines.add("<ArchGuard Tips>: 'Fail to clone source with exitCode 128' => Git Clone 出错，尝试根据: https://archguard.org/faq#git 进行配置")
                }
                line.contains("Failed to scan xxxScanner") -> {
                    lines.add("<ArchGuard Tips>: 'Failed to scan xxxScanner' => 检查 Scanner 是否完整? 版本是否正常? 对应版本映射关系见: https://archguard.org/release/version-mapping")
                }
            }
        } catch (e: RuntimeException) {
            LOG.error("Problem consuming line [{}]", line, e)
        }
    }

    fun asList(): List<String> {
        return ArrayList(lines)
    }

    operator fun contains(message: String?): Boolean {
        return toString().contains(message!!)
    }

    override fun toString(): String {
        return asList().joinToString("\n")
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(InMemoryConsumer::class.java)
    }
}
