package cc.unitmesh.agent.scoring

import cc.unitmesh.devins.document.Entity
import cc.unitmesh.devins.document.TOCItem
import cc.unitmesh.devins.document.DocumentChunk

/**
 * A heuristic-based scoring model that prioritizes code entities over documentation.
 */
class HeuristicScoringModel : ScoringModel {

    override fun scoreAll(segments: List<TextSegment>, query: String): List<Double> {
        return segments.map { segment ->
            calculateScore(segment, query)
        }
    }

    private fun calculateScore(segment: TextSegment, query: String): Double {
        val type = segment.metadata["type"] as? String ?: "unknown"
        val name = segment.metadata["name"] as? String ?: ""
        val content = segment.text

        var score = 0.0

        // Base score by type
        score += when (type) {
            "class" -> 100.0
            "function" -> 90.0
            "heading" -> 60.0
            "toc" -> 40.0
            "chunk" -> 20.0
            else -> 10.0
        }

        // Bonus for exact name match
        if (name.equals(query, ignoreCase = true)) {
            score += 20.0
        } else if (name.contains(query, ignoreCase = true)) {
            score += 10.0
        }

        // Bonus for content match
        if (content.contains(query, ignoreCase = true)) {
            score += 5.0
        }

        return score
    }
}
