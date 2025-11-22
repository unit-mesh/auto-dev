package cc.unitmesh.devins.ui.platform

/**
 * 跨平台文件选择器抽象
 * 基于 FileKit 实现，支持 JVM、Android、iOS、JS/WASM
 */
interface FileChooser {
    /**
     * 选择文件
     * @param title 对话框标题
     * @param initialDirectory 初始目录路径
     * @param fileExtensions 允许的文件扩展名列表，如 listOf("devin", "md")
     * @return 选中的文件路径，如果取消则返回 null
     */
    suspend fun chooseFile(
        title: String = "Choose File",
        initialDirectory: String? = null,
        fileExtensions: List<String>? = null
    ): String?

    /**
     * 选择目录
     * @param title 对话框标题
     * @param initialDirectory 初始目录路径
     * @return 选中的目录路径，如果取消则返回 null
     */
    suspend fun chooseDirectory(
        title: String = "Choose Directory",
        initialDirectory: String? = null
    ): String?

    /**
     * 保存文件
     * @param title 对话框标题
     * @param initialDirectory 初始目录路径
     * @param defaultFileName 默认文件名
     * @param fileExtension 文件扩展名，如 "png", "svg"
     * @param data 要保存的字节数据
     * @return 保存的文件路径，如果取消则返回 null
     */
    suspend fun saveFile(
        title: String = "Save File",
        initialDirectory: String? = null,
        defaultFileName: String,
        fileExtension: String,
        data: ByteArray
    ): String?
}

/**
 * 获取平台特定的文件选择器实例
 * 现在所有平台都使用 FileKit 实现
 */
expect fun createFileChooser(): FileChooser
