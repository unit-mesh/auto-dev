package cc.unitmesh.devti.language.psi

import cc.unitmesh.devti.language.DevInFileType
import cc.unitmesh.devti.language.DevInLanguage
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.*
import java.util.*

class DevInFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, DevInLanguage.INSTANCE) {
    override fun getFileType(): FileType = DevInFileType.INSTANCE

    override fun getOriginalFile(): DevInFile = super.getOriginalFile() as DevInFile

    override fun toString(): String = "DevInFile"

    override fun getStub(): DevInFileStub? = super.getStub() as DevInFileStub?

    companion object {
        /**
         * Create a temp DevInFile from a string.
         */
        fun fromString(project: Project, text: String): DevInFile {
            val filename =
                DevInLanguage.displayName + "-${UUID.randomUUID()}." + DevInFileType.INSTANCE.defaultExtension
            val devInFile = runReadAction {
                PsiFileFactory.getInstance(project).createFileFromText(filename, DevInLanguage, text) as DevInFile
            }

            return devInFile
        }

        fun lookup(project: Project, path: String) = VirtualFileManager.getInstance()
            .findFileByUrl("file://$path")
            ?.let {
                PsiManager.getInstance(project).findFile(it)
            } as? DevInFile
    }
}

