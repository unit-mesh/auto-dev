package cc.unitmesh.devti.template

/**
 *
 * A template role is a string that contains multiple sections.
 * Each section is a string that starts with "```" and ends with "```".
 * The section name is the string between "```" and "```", then will be used as the key of the section.
 * The content of the section is the string between the section name and the next section name.
 *
 * For example:
 *
 * ```system```
 * You are a helpful assistant.
 *
 * ```user```
 * ${question}
 *
 * Will be split to:
 * mapOf(
 *    "system" to "You are a helpful assistant.",
 *    "user" to "${question}"
 * )
 *
 * If the input string does not start with "```" and end with "```", it will be treated as a section named "user".
 */
class TemplateRoleSplitter {
    fun split(input: String): Map<String, String> {
        val sections = mutableMapOf<String, String>()
        val lines = input.lines()
        var currentSection = ""
        val contentBuilder = StringBuilder()

        for (line in lines) {
            if (line.startsWith("```") && line.endsWith("```") && line.length > 6) {
                // Found a section header
                if (currentSection.isNotEmpty()) {
                    sections[currentSection] = contentBuilder.toString()
                    contentBuilder.clear()
                }

                currentSection = line.substring(3, line.length - 3)
            } else {
                // Append line to the current section's content
                contentBuilder.append(line).append("\n")
            }
        }

        // Add the last section if it exists
        if (currentSection.isNotEmpty()) {
            sections[currentSection] = contentBuilder.toString()
        }

        if (sections.isEmpty()) {
            sections["user"] = input
        }

        return sections
    }
}

