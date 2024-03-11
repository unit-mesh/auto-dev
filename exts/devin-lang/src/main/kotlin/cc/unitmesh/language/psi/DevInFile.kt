package cc.unitmesh.language.psi

import cc.unitmesh.language.DevInFileType
import cc.unitmesh.language.DevInLanguage
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider

class DevInFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, DevInLanguage) {
    override fun getFileType(): FileType = DevInFileType.INSTANCE

}
