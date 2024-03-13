package cc.unitmesh.devti.language.psi

import cc.unitmesh.devti.language.DevInFileType
import cc.unitmesh.devti.language.DevInLanguage
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiFile
import com.intellij.psi.StubBuilder
import com.intellij.psi.stubs.*
import com.intellij.psi.tree.IStubFileElementType

class DevInFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, DevInLanguage) {
    override fun getFileType(): FileType = DevInFileType.INSTANCE

    override fun getOriginalFile(): DevInFile = super.getOriginalFile() as DevInFile

    override fun toString(): String = "DevInFile"

    override fun getStub(): DevInFileStub? = super.getStub() as DevInFileStub?
}

class DevInFileStub(file: DevInFile?, private val flags: Int) : PsiFileStubImpl<DevInFile>(file) {

    override fun getType() = Type

    object Type : IStubFileElementType<DevInFileStub>(DevInLanguage) {
        override fun getStubVersion(): Int = 1

        override fun getExternalId(): String = "Feakin.file"

        override fun serialize(stub: DevInFileStub, dataStream: StubOutputStream) {
            dataStream.writeByte(stub.flags)
        }

        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): DevInFileStub {
            return DevInFileStub(null, dataStream.readUnsignedByte())
        }

        override fun getBuilder(): StubBuilder = object : DefaultStubBuilder() {
            override fun createStubForFile(file: PsiFile): StubElement<*> {
                return DevInFileStub(file as DevInFile, 0)
            }
        }
    }
}