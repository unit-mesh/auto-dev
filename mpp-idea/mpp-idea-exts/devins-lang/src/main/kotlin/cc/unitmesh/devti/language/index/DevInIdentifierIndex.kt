package cc.unitmesh.devti.language.index

import cc.unitmesh.devti.language.DevInFileType
import cc.unitmesh.devti.language.ast.HobbitHole
import cc.unitmesh.devti.language.psi.DevInFrontMatterKey
import cc.unitmesh.devti.language.psi.DevInVisitor
import com.intellij.psi.PsiElement
import com.intellij.util.indexing.*
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.KeyDescriptor
import java.io.DataInput
import java.io.DataOutput

internal val SHIRE_CONFIG_IDENTIFIER_INDEX_ID = ID.create<String, Int>("devin.index.name")

internal val isIndexing = ThreadLocal<Boolean>()

class DevInIdentifierIndex: FileBasedIndexExtension<String, Int>() {
    override fun getValueExternalizer() = object : DataExternalizer<Int> {
        override fun save(out: DataOutput, value: Int) = out.writeInt(value)
        override fun read(`in`: DataInput) = `in`.readInt()
    }

    override fun getIndexer() = DataIndexer<String, Int, FileContent> {
        val result = mutableMapOf<String, Int>()
        val visitor = object : DevInVisitor() {
            override fun visitElement(element: PsiElement) {
                if (element is DevInFrontMatterKey && element.text == HobbitHole.NAME) {
                    result[element.text] = element.textOffset
                }

                super.visitElement(element)
            }
        }

        isIndexing.set(true)
        it.psiFile.accept(visitor)
        isIndexing.set(false)
        result
    }

    override fun getName() = SHIRE_CONFIG_IDENTIFIER_INDEX_ID
    override fun getVersion() = 1
    override fun dependsOnFileContent() = true
    override fun getInputFilter() = inputFilter
    override fun getKeyDescriptor(): KeyDescriptor<String> = EnumeratorStringDescriptor.INSTANCE

    private val inputFilter = DefaultFileTypeSpecificInputFilter(DevInFileType.INSTANCE)
}
