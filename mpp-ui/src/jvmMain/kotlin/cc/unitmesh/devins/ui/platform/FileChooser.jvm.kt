package cc.unitmesh.devins.ui.platform

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openDirectoryPicker
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.path
import io.github.vinceglb.filekit.write

/**
 * FileKit 实现的跨平台文件选择器 - JVM 平台
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

        val directory = initialDirectory?.let { PlatformFile(it) }

        val file = FileKit.openFilePicker(
            type = fileType,
            title = title,
            directory = directory
        )

        return file?.path
    }

    override suspend fun chooseDirectory(
        title: String,
        initialDirectory: String?
    ): String? {
        val directory = initialDirectory?.let { PlatformFile(it) }

        val result = FileKit.openDirectoryPicker(
            title = title,
            directory = directory
        )

        return result?.path
    }

    override suspend fun saveFile(
        title: String,
        initialDirectory: String?,
        defaultFileName: String,
        fileExtension: String,
        data: ByteArray
    ): String? {
        return try {
            // 使用 FileKit 的跨平台文件保存对话框
            val directory = initialDirectory?.let { PlatformFile(it) }

            // 从 defaultFileName 中提取文件名（不含扩展名）
            val suggestedName = defaultFileName.substringBeforeLast(".$fileExtension")
                .substringBeforeLast(".")

            val file = FileKit.openFileSaver(
                suggestedName = suggestedName,
                extension = fileExtension,
                directory = directory
            )

            if (file != null) {
                // 使用 FileKit 的 write 扩展函数写入数据
                file.write(data)
                file.path
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

actual fun createFileChooser(): FileChooser = FileKitChooser()

