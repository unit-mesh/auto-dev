package cc.unitmesh.devins.ui.platform

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.name

/**
 * FileKit implementation for WASM platform
 * 
 * Reference: https://filekit.mintlify.app/dialogs/file-picker
 * 
 * Supported features:
 * - ✅ File picker (WASM supported) - https://filekit.mintlify.app/dialogs/file-picker
 * - ❌ Directory picker (WASM NOT supported) - https://filekit.mintlify.app/dialogs/directory-picker
 *      Only supported on: Android, iOS, macOS, JVM
 * 
 * Note: On WASM/browser platform:
 * - File paths are virtual, only file names are accessible
 * - initialDirectory parameter is not supported
 * - Full file system access is restricted due to browser security
 */
class FileKitChooser : FileChooser {
    override suspend fun chooseFile(
        title: String,
        initialDirectory: String?,
        fileExtensions: List<String>?
    ): String? {
        // Configure file type filter
        // Reference: https://filekit.mintlify.app/dialogs/file-picker#filter-by-type
        val fileType = if (fileExtensions.isNullOrEmpty()) {
            FileKitType.File()
        } else {
            FileKitType.File(fileExtensions)
        }

        // Open file picker with FileKit API
        // WASM is officially supported: https://filekit.mintlify.app/dialogs/file-picker
        val file = FileKit.openFilePicker(
            type = fileType,
            title = title
            // initialDirectory is not supported on WASM/browser platform
        )

        // On WASM/browser, only file name is accessible due to security restrictions
        // Full file path is not available in browser environment
        return file?.name
    }

    override suspend fun chooseDirectory(
        title: String,
        initialDirectory: String?
    ): String? {
        // Directory picker is NOT supported on WASM platform
        // Reference: https://filekit.mintlify.app/dialogs/directory-picker
        // Supported platforms: Android, iOS, macOS, JVM (NOT JS/WASM)
        return null
    }
}

actual fun createFileChooser(): FileChooser = FileKitChooser()

