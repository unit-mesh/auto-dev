package cc.unitmesh.devti.custom.team

import cc.unitmesh.cf.core.llms.LlmMsg
import cc.unitmesh.template.TemplateRoleSplitter
import org.yaml.snakeyaml.Yaml

data class TeamActionPrompt(
    var interaction: InteractionType = InteractionType.AppendCursorStream,
    var priority: Int = 0,
    var other: Map<String, Any> = mapOf(),
    // the rest of content is the chat messages
    var msgs: List<LlmMsg.ChatMessage> = listOf(),
) {
    companion object {
        /**
         * Parses the given content and returns a TeamActionPrompt object.
         *
         * @param content The content to be parsed, which includes front-matter and chat messages.
         * @return A TeamActionPrompt object containing the parsed information.
         *
         * Example usage:
         * ```
         * ---
         * interaction: AppendCursorStream
         * priority: 1
         * key1: value1
         * key2: value2
         * ---
         * ```system```
         * Chat message 1
         * ```user```
         * Chat message 2
         * ```
         *
         * This method parses the provided content and constructs a TeamActionPrompt object with the following properties:
         * - interaction: The interaction type specified in the front-matter.
         * - priority: The priority value specified in the front-matter.
         * - other: A map containing any additional key-value pairs specified in the front-matter, excluding "interaction" and "priority".
         * - msgs: A list of LlmMsg.ChatMessage objects representing the parsed chat messages.
         *
         * The content is expected to have a specific format. It should start with front-matter enclosed in triple dashes (---),
         * followed by a newline, and then the chat messages. The front-matter should be in YAML format, with each key-value pair
         * on a separate line. The chat messages should be enclosed in triple backticks (```) and should have a role specifier
         * (e.g., "system" or "user") followed by the message content on a new line. Each chat message should be separated by a newline.
         *
         * If the content does not contain front-matter, the method assumes that the entire content is chat messages and parses it accordingly.
         *
         * Example:
         * ```
         * TeamActionPrompt(
         *    interaction=AppendCursorStream,
         *    priority=1,
         *    other={key1=value1, key2=value2},
         *    msgs=[
         *    LlmMsg.ChatMessage(role=System, content=Chat message 1\n, cursor=null),
         *    LlmMsg.ChatMessage(role=User, content=Chat message 2\n, cursor=null)
         *   ]
         * )
         * ```
         */
        fun fromContent(content: String): TeamActionPrompt {
            val regex = """^---\s*\n(.*?)\n---\s*\n(.*)$""".toRegex(RegexOption.DOT_MATCHES_ALL)
            val matchResult = regex.find(content)

            val prompt = TeamActionPrompt()
            if (matchResult != null) {
                val frontMatter = matchResult.groupValues[1]
                val yaml = Yaml()
                val frontMatterMap = yaml.load<Map<String, Any>>(frontMatter)
                prompt.interaction = try {
                    InteractionType.valueOf(frontMatterMap["interaction"] as String)
                } catch (e: Exception) {
                    InteractionType.AppendCursorStream
                }

                prompt.priority = try {
                    frontMatterMap["priority"] as Int
                } catch (e: Exception) {
                    0
                }

                prompt.other = frontMatterMap.filterKeys { it != "interaction" && it != "priority" }

                val chatContent = matchResult?.groupValues?.get(2) ?: content
                prompt.msgs = parseChatMessage(chatContent)
            } else {
                prompt.msgs = parseChatMessage(content)
            }

            // the rest of content is the chat messages
            return prompt
        }

        private fun parseChatMessage(chatContent: String): List<LlmMsg.ChatMessage> {
            try {
                val msgs = TemplateRoleSplitter().split(chatContent)
                val messages = LlmMsg.fromMap(msgs).toMutableList()

                return messages
            } catch (e: Exception) {
                return listOf(LlmMsg.ChatMessage(LlmMsg.ChatRole.User, chatContent, null))
            }
        }
    }
}

enum class InteractionType {
    ChatPanel,
    AppendCursor,
    AppendCursorStream,
    ;
}