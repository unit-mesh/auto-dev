package cc.unitmesh.devins.ui.platform

import androidx.activity.ComponentActivity
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openDirectoryPicker
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.path

/**
 * FileKit 实现的跨平台文件选择器 - Android 平台
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
}

actual fun createFileChooser(): FileChooser = FileKitChooser()



/**
 * Android 上的 Activity 提供者
 * 需要在应用启动时设置 Activity 实例并初始化 FileChooser
 */
object AndroidActivityProvider {
    private var activity: ComponentActivity? = null

    fun setActivity(activity: ComponentActivity) {
        this.activity = activity
    }

    fun getActivity(): ComponentActivity? = activity
}
