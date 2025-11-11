package cc.unitmesh.devins.ui.platform

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.name

/**
 * FileKit 实现的跨平台文件选择器 - JS 平台
 * 注意：JS 平台不支持目录选择，且只能返回文件名而非完整路径
 */
class FileKitChooser : FileChooser {
    override suspend fun chooseFile(
        title: String,
        initialDirectory: String?,
        fileExtensions: List<String>?
    ): String? {
        val fileType = if (fileExtensions.isNullOrEmpty()) {
            FileKitType.File()
        } else {
            FileKitType.File(fileExtensions)
        }

        val file = FileKit.openFilePicker(
            type = fileType,
            title = title
        )

        // JS 平台只能返回文件名，无法获取完整路径
        return file?.name
    }

    override suspend fun chooseDirectory(
        title: String,
        initialDirectory: String?
    ): String? {
        // JS 平台不支持目录选择
        return null
    }
}

actual fun createFileChooser(): FileChooser = FileKitChooser()

