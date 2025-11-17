package cc.unitmesh.viewer

/**
 * Types of content that can be displayed in the viewer
 */
enum class ViewerType {
    /**
     * Code/text content with syntax highlighting (Monaco Editor)
     */
    CODE,

    /**
     * Markdown content with rendered preview
     */
    MARKDOWN,

    /**
     * Image content
     */
    IMAGE,

    /**
     * PDF document
     */
    PDF,

    /**
     * Video content
     */
    VIDEO,

    /**
     * Audio content
     */
    AUDIO,

    /**
     * Generic binary or unsupported content
     */
    UNSUPPORTED
}

