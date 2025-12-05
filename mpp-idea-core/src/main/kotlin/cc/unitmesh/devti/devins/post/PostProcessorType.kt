package cc.unitmesh.devti.devins.post

/**
 * Post middleware actions, like
 * Logging, Metrics, CodeVerify, RunCode, ParseCode etc.
 *
 */
enum class PostProcessorType(var handleName: String) {
    /**
     * Metric time spent on the action.
     */
    TimeMetric("timeMetric"),

    /**
     * Check has code error or PSI issue.
     */
    VerifyCode("verifyCode"),

    /**
     * Run generate text code
     */
    RunCode("runCode"),

    /**
     * Parse text to code blocks
     */
    ParseCode("parseCode"),

    /**
     * Save file to the disk
     */
    SaveFile("saveFile"),

    /**
     * Open file in the editor
     */
    OpenFile("openFile"),

    /**
     * Insert code to the editor by current cursor position.
     */
    InsertCode("insertCode"),

    /**
     * Format code
     */
    FormatCode("formatCode"),

    /**
     * Parse comment to the comment block
     */
    ParseComment("parseComment"),

    /**
     * Insert new line
     */
    InsertNewline("insertNewline"),

    /**
     * Append text to the file
     */
    Append("append"),

    /**
     * Patch content to the file
     */
    Patch("patch"),

    /**
     * Diff
     */
    Diff("diff"),

    UpdateEditorText("updateEditorText"),

    /**
     * openWebpage
     */
    OpenWebpage("openWebpage"),

    /**
     *  showWebView
     */
    ShowWebview("showWebView"),
    ;
}