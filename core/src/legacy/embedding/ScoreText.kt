package cc.unitmesh.devti.embedding

import com.intellij.openapi.vfs.VirtualFile

data class ScoredText(
    val text: String,
    val similarity: Double = 0.0,
    var index: Int = 0,
    var count: Int = 0,
    val file: VirtualFile? = null,
    var embedding: FloatArray? = null,
) {
    override fun toString(): String {
        return "Similarity: ${similarity}, Text: $text"
    }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ScoredText

        if (text != other.text) return false
        if (index != other.index) return false
        if (count != other.count) return false
        if (similarity != other.similarity) return false
        if (file != other.file) return false
        if (embedding != null) {
            if (other.embedding == null) return false
            if (!embedding.contentEquals(other.embedding)) return false
        } else if (other.embedding != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = text.hashCode()
        result = 31 * result + index
        result = 31 * result + count
        result = 31 * result + similarity.hashCode()
        result = 31 * result + (file?.hashCode() ?: 0)
        result = 31 * result + (embedding?.contentHashCode() ?: 0)
        return result
    }
}
