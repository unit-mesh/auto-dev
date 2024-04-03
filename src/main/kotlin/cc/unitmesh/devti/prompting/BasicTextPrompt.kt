package cc.unitmesh.devti.prompting

data class BasicTextPrompt(
    /**
     * The text to display to the user
     */
    val displayText: String,
    /**
     * The text request to the server
     */
    val requestText: String
)