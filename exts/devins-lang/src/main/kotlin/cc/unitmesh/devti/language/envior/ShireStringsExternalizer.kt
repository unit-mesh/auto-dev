package cc.unitmesh.devti.language.envior

import com.intellij.util.io.DataExternalizer
import java.io.DataInput
import java.io.DataOutput

class ShireStringsExternalizer : DataExternalizer<Set<String>> {
    override fun save(out: DataOutput, value: Set<String>) {
        out.writeInt(value.size)
        for (s in value) {
            out.writeUTF(s)
        }
    }

    override fun read(input: DataInput): Set<String> {
        val size = input.readInt()
        val result: MutableSet<String> = HashSet(size)
        for (i in 0 until size) {
            result.add(input.readUTF())
        }

        return result
    }

}