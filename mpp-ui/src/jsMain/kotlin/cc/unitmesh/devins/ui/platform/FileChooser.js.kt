package cc.unitmesh.devins.ui.platform

/**
 * JS 平台的文件选择器实现
 * 目前提供空实现，未来可以基于 HTML5 File API 实现
 */
class JsFileChooser : FileChooser {
    override suspend fun chooseFile(
        title: String,
        initialDirectory: String?,
        fileExtensions: List<String>?
    ): String? {
        console.warn("File chooser not implemented for JS platform")
        return null
    }

    override suspend fun chooseDirectory(
        title: String,
        initialDirectory: String?
    ): String? {
        console.warn("Directory chooser not implemented for JS platform")
        return null
    }
}

actual fun createFileChooser(): FileChooser = JsFileChooser()
