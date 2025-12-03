package cc.unitmesh.agent.plan

import kotlinx.serialization.Serializable

/**
 * Task status enum for plan steps and tasks.
 * Matches the markers used in markdown plan format:
 * - [ ] TODO
 * - [*] IN_PROGRESS
 * - [✓] or [x] COMPLETED
 * - [!] FAILED
 * - BLOCKED (no standard marker, used programmatically)
 */
@Serializable
enum class TaskStatus {
    TODO,
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    BLOCKED;

    companion object {
        /**
         * Parse status from markdown marker character
         */
        fun fromMarker(marker: String): TaskStatus = when (marker.trim().lowercase()) {
            "x", "✓" -> COMPLETED
            "!" -> FAILED
            "*" -> IN_PROGRESS
            "" , " " -> TODO
            else -> TODO
        }

        /**
         * Get the markdown marker for this status
         */
        fun TaskStatus.toMarker(): String = when (this) {
            COMPLETED -> "✓"
            FAILED -> "!"
            IN_PROGRESS -> "*"
            TODO -> " "
            BLOCKED -> "B"
        }
    }
}

/**
 * Plan phase enum following PDCA cycle.
 * Used to track the overall phase of a task.
 */
@Serializable
enum class PlanPhase {
    PLAN,   // Planning phase - analyzing and designing
    DO,     // Execution phase - implementing changes
    CHECK,  // Verification phase - testing and reviewing
    ACT     // Action phase - finalizing and deploying
}

