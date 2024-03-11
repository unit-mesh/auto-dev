package cc.unitmesh.devti.agent.model

import kotlinx.serialization.Serializable

typealias Embedding = List<Float>

/**
 * `StandardTextChunk` is a data class that represents a chunk of text within a document. It includes the following fields:
 *
 * - `id`: A string that uniquely identifies the text chunk.
 * - `text`: The actual text content of the chunk.
 * - `embedding`: An `Embedding` object that contains the vector representation of the text chunk.
 * - `score`: A floating-point number that represents the score or importance of the text chunk.
 */
@Serializable
data class StandardTextChunk(
    val id: String?,
    val text: String?,
    val embedding: Embedding?,
    val score: Float?,
)
