package cc.unitmesh.devti.language.envior

import com.intellij.json.JsonFileType
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.indexing.DefaultFileTypeSpecificInputFilter

class ShireEnvironmentInputFilter : DefaultFileTypeSpecificInputFilter(*arrayOf<FileType>(JsonFileType.INSTANCE)) {
    override fun acceptInput(file: VirtualFile): Boolean {
        return super.acceptInput(file) && isShireEnvFile(file)
    }

    private fun isShireEnvFile(file: VirtualFile?): Boolean {
        return file?.name?.endsWith(".shireEnv.json") ?: false
    }
}