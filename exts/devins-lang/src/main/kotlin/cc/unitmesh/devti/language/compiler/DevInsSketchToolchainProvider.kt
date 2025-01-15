package cc.unitmesh.devti.language.compiler

import cc.unitmesh.devti.devin.dataprovider.BuiltinCommand
import cc.unitmesh.devti.sketch.SketchToolchainProvider
import cc.unitmesh.devti.sketch.Toolchain

class DevInsSketchToolchainProvider : SketchToolchainProvider {
    override fun collect(): List<Toolchain> {
        return BuiltinCommand.all().map {
            val example = BuiltinCommand.example(it)
            Toolchain(it.commandName, it.description, example)
        }
    }
}
