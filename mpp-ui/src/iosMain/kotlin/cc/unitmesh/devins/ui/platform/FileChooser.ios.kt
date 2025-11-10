package cc.unitmesh.devins.ui.platform

/**
 * iOS implementation of file chooser
 * File chooser functionality is limited on iOS
 */
actual fun createFileChooser(): FileChooser {
    return IosFileChooser()
}

/**
 * iOS implementation of FileChooser
 * Uses UIDocumentPickerViewController (stub for now)
 */
class IosFileChooser : FileChooser {
    override suspend fun chooseFile(
        title: String,
        initialDirectory: String?,
        fileExtensions: List<String>?
    ): String? {
        // iOS file chooser implementation would require UIDocumentPickerViewController
        // For now, this is a stub implementation
        println("File chooser not yet implemented for iOS")
        return null
    }

    override suspend fun chooseDirectory(
        title: String,
        initialDirectory: String?
    ): String? {
        // iOS directory chooser implementation would require UIDocumentPickerViewController
        // For now, this is a stub implementation
        println("Directory chooser not yet implemented for iOS")
        return null
    }
}

