package cc.unitmesh.devti.command.dataprovider

/**
 * Represents the frontmatter section of a skill file (SpecKit or Claude Skills).
 *
 * Skill files (e.g., speckit.plan.prompt.md or SKILL.md) can contain YAML frontmatter
 * with metadata and variable definitions.
 *
 * Example:
 * ```yaml
 * ---
 * name: PDF Skill
 * description: Execute the implementation planning workflow
 * variables:
 *   FEATURE_SPEC: "path/to/spec.md"
 *   IMPL_PLAN: "path/to/plan.md"
 * ---
 * ```
 */
data class SkillFrontmatter(
    /**
     * Name of the skill (used for Claude Skills)
     */
    val name: String = "",

    /**
     * Description of what this skill does
     */
    val description: String = "",

    /**
     * Variable definitions that can be used in the template.
     * Key: variable name (e.g., "FEATURE_SPEC")
     * Value: variable value or path to load content from
     */
    val variables: Map<String, Any> = emptyMap(),

    /**
     * Any additional frontmatter fields not explicitly defined above
     */
    val additionalFields: Map<String, Any> = emptyMap()
) {
    companion object {
        /**
         * Parse frontmatter from a markdown string.
         * Returns null if no frontmatter is found.
         */
        fun parse(markdown: String): Pair<SkillFrontmatter?, String> {
            val frontmatterRegex = Regex("^---\\s*\\n(.*?)\\n---\\s*\\n", RegexOption.DOT_MATCHES_ALL)
            val match = frontmatterRegex.find(markdown)

            if (match == null) {
                return Pair(null, markdown)
            }

            val yamlContent = match.groupValues[1]
            val contentWithoutFrontmatter = markdown.substring(match.range.last + 1)

            return try {
                val frontmatter = parseYaml(yamlContent)
                Pair(frontmatter, contentWithoutFrontmatter)
            } catch (e: Exception) {
                // If YAML parsing fails, return null frontmatter but keep the content
                Pair(null, contentWithoutFrontmatter)
            }
        }

        /**
         * Parse YAML content into SkillFrontmatter
         */
        private fun parseYaml(yamlContent: String): SkillFrontmatter {
            val yaml = org.yaml.snakeyaml.Yaml()
            val data = yaml.load<Map<String, Any>>(yamlContent) ?: emptyMap()

            val name = data["name"]?.toString() ?: ""
            val description = data["description"]?.toString() ?: ""

            @Suppress("UNCHECKED_CAST")
            val variables = (data["variables"] as? Map<String, Any>) ?: emptyMap()

            // Collect any additional fields
            val additionalFields = data.filterKeys { it !in listOf("name", "description", "variables") }

            return SkillFrontmatter(
                name = name,
                description = description,
                variables = variables,
                additionalFields = additionalFields
            )
        }
    }
}

/**
 * Type alias for backward compatibility with existing SpecKit code
 */
@Deprecated("Use SkillFrontmatter instead", ReplaceWith("SkillFrontmatter"))
typealias SpecKitFrontmatter = SkillFrontmatter

