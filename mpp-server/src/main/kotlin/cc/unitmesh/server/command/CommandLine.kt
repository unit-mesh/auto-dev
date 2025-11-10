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

import java.io.File
import java.util.function.Consumer

class CommandLine(val executable: String) {
    private var workingDir: File? = null
    private lateinit var encoding: String
    private var secrets: List<String> = listOf()
    private var arguments: MutableList<String> = mutableListOf()

    companion object {
        fun createCommandLine(command: String): CommandLine {
            return CommandLine(command)
        }
    }

    fun `when`(condition: Boolean, thenDo: Consumer<CommandLine>): CommandLine {
        return this.tap { cmd: CommandLine ->
            if (condition) {
                thenDo.accept(cmd)
            }
        }
    }

    fun tap(thenDo: Consumer<CommandLine>): CommandLine {
        thenDo.accept(this)
        return this
    }

    fun withNonArgSecrets(secrets: List<String>): CommandLine {
        this.secrets += secrets
        return this
    }

    fun withArg(argument: String): CommandLine {
        this.arguments += argument
        return this
    }

    fun withArgs(vararg args: String): CommandLine {
        for (arg in args) {
            arguments.add(arg)
        }
        return this
    }

    fun withWorkingDir(workingDir: File?): CommandLine {
        this.workingDir = workingDir
        return this
    }

    fun withEncoding(encoding: String): CommandLine {
        this.encoding = encoding
        return this
    }

    fun getCommandLine(): List<String> {
        val args: MutableList<String> = ArrayList()
        args.add(executable)
        for (i in arguments.indices) {
            val argument = arguments[i]
            args.add(argument)
        }

        return args.toList()
    }
}
