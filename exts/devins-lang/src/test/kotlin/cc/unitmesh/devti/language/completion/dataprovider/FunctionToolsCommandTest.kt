package cc.unitmesh.devti.language.completion.dataprovider

import cc.unitmesh.devti.devin.dataprovider.BuiltinCommand
import cc.unitmesh.devti.provider.toolchain.ToolchainFunctionProvider
import com.intellij.testFramework.LightPlatformTestCase

class FunctionToolsCommandTest : LightPlatformTestCase() {
    fun testShouldLoadAllBridgeCommandExamples() {
        val functions = ToolchainFunctionProvider.all()

        val map = functions.map {
            it.funcNames().map { name ->
                BuiltinCommand.example(name)
            }
        }

        println(map)
    }
}