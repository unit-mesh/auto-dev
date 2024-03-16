package cc.unitmesh.devti.language.compiler.model

data class LineInfo(val startLine: Int, val endLine: Int) {
    companion object {
        /**
         * Convert a string to a TextRange, if possible.
         * format: "filepath#L1-L12" means from line 1 to line 12
         */
        fun fromString(string: String): LineInfo? {
            val lineRange = string.substringAfter('#').split('-')
            if (lineRange.size != 2) {
                return null
            }

            val start = lineRange[0].substringAfter('L')
            val end = lineRange[1].substringAfter('L')

            // use regex to check if the start is a number
            if (!start.matches(Regex("\\d+"))) {
                return null
            }

            if (!end.matches(Regex("\\d+"))) {
                return null
            }

            val startLine = start.toInt()
            val endLine = end.toInt()

            return LineInfo(startLine, endLine)
        }
    }

}