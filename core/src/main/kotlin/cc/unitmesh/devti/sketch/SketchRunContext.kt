package cc.unitmesh.devti.sketch

import com.intellij.openapi.vfs.VirtualFile

data class SketchRunContext(
    // Current File
    @JvmField val file: VirtualFile,
    /// related files
    @JvmField val userSelectFile: List<VirtualFile>,
    /// ast related files
    @JvmField val astRelatedFiles: List<VirtualFile>,
    // The absolute path of the USER's workspace
    @JvmField val workspace: String,
    // The USER's OS
    @JvmField val os: String,
    // The current time in YYYY-MM-DD HH:MM:SS format
    @JvmField val time: String,
    /// The USER's requirements
    @JvmField val input: String,
    /// toolList
    @JvmField val toolList: List<Toolchain>
) {

}

/**
 * todo use [cc.unitmesh.devti.language.compiler.exec.InsCommand] to run the sketch
 */
enum class Toolchain(open val commandName: String, open val description: String) {

}