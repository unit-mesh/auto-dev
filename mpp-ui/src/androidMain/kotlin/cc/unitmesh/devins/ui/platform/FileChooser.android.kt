package cc.unitmesh.devins.ui.platform

/**
 * Android 平台的文件选择器实现
 * 目前提供空实现，未来可以基于 Android Storage Access Framework 实现
 */
class AndroidFileChooser : FileChooser {
    
    override suspend fun chooseFile(
        title: String,
        initialDirectory: String?,
        fileExtensions: List<String>?
    ): String? {
        // TODO: 使用 Intent.ACTION_OPEN_DOCUMENT 实现
        return null
    }
    
    override suspend fun chooseDirectory(
        title: String,
        initialDirectory: String?
    ): String? {
        // TODO: 使用 Intent.ACTION_OPEN_DOCUMENT_TREE 实现
        return null
    }
}

actual fun createFileChooser(): FileChooser = AndroidFileChooser()

