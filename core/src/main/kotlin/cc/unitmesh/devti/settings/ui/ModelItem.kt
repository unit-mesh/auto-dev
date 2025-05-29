package cc.unitmesh.devti.settings.ui

/**
 * Data class to store both display name and model ID for dropdown items
 */
data class ModelItem(val displayName: String, val modelId: String, val isCustom: Boolean = false) {
    override fun toString(): String = displayName
}