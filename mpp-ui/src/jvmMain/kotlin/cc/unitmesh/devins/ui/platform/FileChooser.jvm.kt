package cc.unitmesh.devins.ui.platform

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

/**
 * JVM 平台的文件选择器实现
 */
class JvmFileChooser : FileChooser {
    override suspend fun chooseFile(
        title: String,
        initialDirectory: String?,
        fileExtensions: List<String>?
    ): String? =
        withContext(Dispatchers.IO) {
            val chooser =
                JFileChooser().apply {
                    dialogTitle = title
                    currentDirectory = initialDirectory?.let { File(it) } ?: File(System.getProperty("user.home"))

                    if (!fileExtensions.isNullOrEmpty()) {
                        val description = fileExtensions.joinToString(", ") { "*.$it" }
                        fileFilter = FileNameExtensionFilter(description, *fileExtensions.toTypedArray())
                    }
                }

            val result = chooser.showOpenDialog(null)
            if (result == JFileChooser.APPROVE_OPTION) {
                chooser.selectedFile.absolutePath
            } else {
                null
            }
        }

    override suspend fun chooseDirectory(
        title: String,
        initialDirectory: String?
    ): String? =
        withContext(Dispatchers.IO) {
            val chooser =
                JFileChooser().apply {
                    fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                    dialogTitle = title
                    currentDirectory = initialDirectory?.let { File(it) } ?: File(System.getProperty("user.home"))
                }

            val result = chooser.showOpenDialog(null)
            if (result == JFileChooser.APPROVE_OPTION) {
                chooser.selectedFile.absolutePath
            } else {
                null
            }
        }
}

actual fun createFileChooser(): FileChooser = JvmFileChooser()
