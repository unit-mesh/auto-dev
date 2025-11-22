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
 * FileKit 实现的跨平台文件选择器 - iOS 平台
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
        val directory = initialDirectory?.let { PlatformFile(it) }
        
        // Extract filename without extension
        val baseName = defaultFileName.substringBeforeLast(".")
        
        val file = FileKit.openFileSaver(
            suggestedName = baseName,
            extension = fileExtension,
            directory = directory
        ) ?: return null

        file.write(data)
        return file.path
    }
}

actual fun createFileChooser(): FileChooser = FileKitChooser()

