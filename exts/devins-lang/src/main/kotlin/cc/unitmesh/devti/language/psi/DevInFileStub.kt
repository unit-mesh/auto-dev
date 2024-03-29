package cc.unitmesh.devti.language.psi

import cc.unitmesh.devti.language.DevInLanguage
import com.intellij.psi.PsiFile
import com.intellij.psi.StubBuilder
import com.intellij.psi.stubs.*
import com.intellij.psi.tree.IStubFileElementType

class DevInFileStub(file: DevInFile?, private val flags: Int) : PsiFileStubImpl<DevInFile>(file) {
    override fun getType() = Type

    object Type : IStubFileElementType<DevInFileStub>(DevInLanguage) {
        override fun getStubVersion(): Int = 1

        override fun getExternalId(): String = "devin.file"

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