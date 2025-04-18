// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package cc.unitmesh.devti.embedding

import kotlin.collections.asSequence
import kotlin.math.sqrt
import kotlin.sequences.filter
import kotlin.sequences.map
import kotlin.sequences.sortedByDescending
import kotlin.sequences.take
import kotlin.sequences.toList
import kotlin.to

interface EmbeddingSearchIndex {
    val size: Int
    var limit: Int?

    operator fun contains(id: String): Boolean
    fun clear()

    fun onIndexingStart()
    fun onIndexingFinish()

    suspend fun addEntries(values: Iterable<Pair<String, FloatArray>>, shouldCount: Boolean = false)

    suspend fun saveToDisk()
    suspend fun loadFromDisk()

    fun findClosest(searchEmbedding: FloatArray, topK: Int, similarityThreshold: Double? = null): List<ScoredText>
    fun streamFindClose(searchEmbedding: FloatArray, similarityThreshold: Double? = null): Sequence<ScoredText>

    fun estimateMemoryUsage(): Long
    fun estimateLimitByMemory(memory: Long): Int
    fun checkCanAddEntry(): Boolean
}

internal fun Map<String, FloatArray>.findClosest(
    searchEmbedding: FloatArray,
    topK: Int, similarityThreshold: Double?,
): List<ScoredText> {
    return asSequence()
        .map {
            it.key to searchEmbedding.times(it.value)
        }
        .filter { (_, similarity) -> if (similarityThreshold != null) similarity > similarityThreshold else true }
        .sortedByDescending { (_, similarity) -> similarity }
        .take(topK)
        .map { (id, similarity) -> ScoredText(id, similarity.toDouble()) }
        .toList()
}

internal fun Sequence<Pair<String, FloatArray>>.streamFindClose(
    queryEmbedding: FloatArray,
    similarityThreshold: Double?,
): Sequence<ScoredText> {
    return map { (id, embedding) -> id to queryEmbedding.times(embedding) }
        .filter { similarityThreshold == null || it.second > similarityThreshold }
        .map { (id, similarity) -> ScoredText(id, similarity.toDouble()) }
}

fun FloatArray.times(other: FloatArray): Float {
    require(this.size == other.size) {
        "Embeddings must have the same size, but got ${this.size} and ${other.size}"
    }
    return this.zip(other).map { (a, b) -> a * b }.sum()
}

fun FloatArray.normalized(): FloatArray {
    val norm = sqrt(this.times(this))
    val normalizedValues = this.map { it / norm }
    return normalizedValues.toFloatArray()
}

fun FloatArray.cosine(other: FloatArray): Float {
    require(this.size == other.size) { "Embeddings must have the same size" }
    val dot = this.times(other)
    val norm = sqrt(this.times(this)) * sqrt(other.times(other))
    return dot / norm
}
