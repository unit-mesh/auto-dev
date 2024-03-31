package cc.unitmesh.devti.language.compiler.model

data class LineInfo(
    val startLine: Int,
    val endLine: Int,
    val startColumn: Int = 0,
    val endColumn: Int = 0
) {
    companion object {
        private val regex = Regex("""L(\d+)(?:C(\d+))?(?:-L(\d+)(?:C(\d+))?)?""")

        /**
         * Convert a string to a `TextRange`, if possible. The string should be in the format: "filepath#L1-L12",
         * where "filepath" is the path to the file, "#" is a hash symbol, "L1-L12" is a range of lines from line 1 to line 12.
         *
         * @param string The string to convert, in the format "filepath#L1-L12".
         * @return A `LineInfo` object representing the range of lines, or `null` if the string is not in the correct format.
         */
        fun fromString(input: String): LineInfo? {
            val matchResult = regex.find(input) ?: return null

            val startLine = matchResult.groupValues[1].toIntOrNull() ?: return null
            val startColumn = matchResult.groupValues[2].toIntOrNull() ?: 0
            val endLine = matchResult.groupValues[3].toIntOrNull() ?: return null
            val endColumn = matchResult.groupValues[4].toIntOrNull() ?: 0

            return LineInfo(startLine, endLine, startColumn, endColumn)
        }
    }

}