package cc.unitmesh.devins.command

import cc.unitmesh.devins.filesystem.ProjectFileSystem
import cc.unitmesh.yaml.YamlUtils

/**
 * SpecKitCommand 表示从 .github/prompts/ 目录加载的 GitHub Spec-Kit 命令
 * 
 * 每个命令对应一个提示文件，如 `speckit.clarify.prompt.md`，可以在 DevIns 脚本中
 * 使用 `/speckit.clarify <arguments>` 的形式调用
 */
data class SpecKitCommand(
    val subcommand: String,
    val description: String,
    val template: String
) {
    val fullCommandName: String get() = "speckit.$subcommand"

    companion object {
        private const val PROMPTS_DIR = ".github/prompts"
        private const val SPECKIT_PREFIX = "speckit."
        private const val PROMPT_SUFFIX = ".prompt.md"

        /**
         * 从文件系统加载所有 SpecKit 命令
         */
        fun loadAll(fileSystem: ProjectFileSystem): List<SpecKitCommand> {
            println("🔍 [SpecKitCommand] Loading SpecKit commands...")
            val projectPath = fileSystem.getProjectPath()
            println("🔍 [SpecKitCommand] Project path: $projectPath")
            
            if (projectPath == null) {
                println("⚠️ [SpecKitCommand] Project path is null, returning empty list")
                return emptyList()
            }
            
            val promptsDir = "$PROMPTS_DIR"
            println("🔍 [SpecKitCommand] Looking for prompts in: $promptsDir")
            
            if (!fileSystem.exists(promptsDir)) {
                println("⚠️ [SpecKitCommand] Prompts directory does not exist: $promptsDir")
                return emptyList()
            }
            
            println("✅ [SpecKitCommand] Prompts directory exists!")

            return try {
                val pattern = "$SPECKIT_PREFIX*$PROMPT_SUFFIX"
                println("🔍 [SpecKitCommand] Looking for files matching: $pattern")
                val files = fileSystem.listFiles(promptsDir, pattern)
                println("🔍 [SpecKitCommand] Found ${files.size} matching files: $files")
                
                files.mapNotNull { fileName ->
                        try {
                            println("🔍 [SpecKitCommand] Processing file: $fileName")
                            val subcommand = fileName
                                .removePrefix(SPECKIT_PREFIX)
                                .removeSuffix(PROMPT_SUFFIX)
                            
                            println("🔍 [SpecKitCommand] Extracted subcommand: $subcommand")

                            if (subcommand.isEmpty()) {
                                println("⚠️ [SpecKitCommand] Subcommand is empty, skipping")
                                return@mapNotNull null
                            }

                            val filePath = "$promptsDir/$fileName"
                            println("🔍 [SpecKitCommand] Reading file: $filePath")
                            val template = fileSystem.readFile(filePath)
                            
                            if (template == null) {
                                println("⚠️ [SpecKitCommand] Failed to read file: $filePath")
                                return@mapNotNull null
                            }
                            
                            println("✅ [SpecKitCommand] Successfully read file (${template.length} chars)")
                            val description = extractDescription(template, subcommand)

                            val cmd = SpecKitCommand(
                                subcommand = subcommand,
                                description = description,
                                template = template
                            )
                            println("✅ [SpecKitCommand] Created command: ${cmd.fullCommandName}")
                            cmd
                        } catch (e: Exception) {
                            println("❌ [SpecKitCommand] Error processing file $fileName: ${e.message}")
                            e.printStackTrace()
                            null
                        }
                    }
            } catch (e: Exception) {
                emptyList()
            }
        }

        /**
         * 从命令列表中查找指定的子命令
         */
        fun findBySubcommand(commands: List<SpecKitCommand>, subcommand: String): SpecKitCommand? {
            return commands.find { it.subcommand == subcommand }
        }

        /**
         * 从命令列表中查找指定的完整命令名
         */
        fun findByFullName(commands: List<SpecKitCommand>, commandName: String): SpecKitCommand? {
            return commands.find { it.fullCommandName == commandName }
        }

        /**
         * 检查文件系统是否支持 SpecKit（是否存在 .github/prompts 目录）
         */
        fun isAvailable(fileSystem: ProjectFileSystem): Boolean {
            return fileSystem.exists(PROMPTS_DIR)
        }

        /**
         * 从模板中提取描述信息
         * 优先从 frontmatter 中提取，否则使用默认描述
         */
        private fun extractDescription(template: String, subcommand: String): String {
            return try {
                val (frontmatter, _) = parseFrontmatter(template)
                frontmatter?.get("description")?.toString() ?: "SpecKit: $subcommand"
            } catch (e: Exception) {
                "SpecKit: $subcommand"
            }
        }

        /**
         * 解析 frontmatter（YAML 格式）
         * 返回 frontmatter 数据和剩余内容
         */
        private fun parseFrontmatter(markdown: String): Pair<Map<String, Any>?, String> {
            val frontmatterRegex = Regex("^---\\s*\\n([\\s\\S]*?)\\n---\\s*\\n", RegexOption.MULTILINE)
            val match = frontmatterRegex.find(markdown)

            if (match == null) {
                return Pair(null, markdown)
            }

            val yamlContent = match.groups[1]?.value ?: ""
            val endIndex = match.range.last + 1
            val contentWithoutFrontmatter = if (endIndex < markdown.length) {
                markdown.substring(endIndex)
            } else {
                ""
            }

            return try {
                val frontmatter = YamlUtils.load(yamlContent) ?: emptyMap()
                Pair(frontmatter, contentWithoutFrontmatter)
            } catch (e: Exception) {
                Pair(null, contentWithoutFrontmatter)
            }
        }
    }
}

