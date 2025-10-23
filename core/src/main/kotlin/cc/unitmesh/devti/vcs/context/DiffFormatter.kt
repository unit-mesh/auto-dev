package cc.unitmesh.devti.vcs.context

import org.jetbrains.annotations.NotNull

/**
 * Formats and post-processes diff output.
 * Extracted from original DiffSimplifier.postProcess logic.
 */
object DiffFormatter {
    private val revisionRegex = Regex("\\(revision [^)]+\\)")
    private const val lineTip = "\\ No newline at end of file"

    /**
     * Post-process diff string to extract relevant information.
     * This method simplifies diff output by removing unnecessary metadata
     * and consolidating file operations.
     */
    @NotNull
    fun postProcess(@NotNull diffString: String): String {
        val lines = diffString.lines()
        val length = lines.size
        val destination = ArrayList<String>()
        var index = 0

        while (index < lines.size) {
            val line = lines[index]

            // Skip metadata lines
            if (shouldSkipLine(line)) {
                index++
                continue
            }

            // Handle new file
            val newFileResult = handleNewFile(lines, index)
            if (newFileResult != null) {
                destination.add(newFileResult.first)
                index = newFileResult.second
                continue
            }

            // Handle rename
            val renameResult = handleRename(lines, index)
            if (renameResult != null) {
                destination.add(renameResult.first)
                index = renameResult.second
                continue
            }

            // Handle import changes
            val importResult = handleImportChange(lines, index, length)
            if (importResult != null) {
                destination.add(importResult.first)
                index = importResult.second
                continue
            }

            // Handle delete
            val deleteResult = handleDelete(lines, index, length)
            if (deleteResult != null) {
                destination.add(deleteResult.first)
                index = deleteResult.second
                continue
            }

            // Handle file modification markers
            val modifyResult = handleModifyMarkers(lines, index)
            if (modifyResult != null) {
                destination.add(modifyResult.first)
                index = modifyResult.second
                continue
            }

            // Handle regular lines
            if (line.startsWith("---") || line.startsWith("+++")) {
                val result = revisionRegex.replace(line, "").trim()
                if (result.isNotEmpty()) {
                    destination.add(result)
                }
            } else {
                if (line.trim().isNotEmpty()) {
                    destination.add(line)
                }
            }

            index++
        }

        return destination.joinToString("\n")
    }

    private fun shouldSkipLine(line: String): Boolean {
        return line.startsWith("diff --git ") ||
                line.startsWith("index:") ||
                line.startsWith("Index:") ||
                line == "===================================================================" ||
                line.contains(lineTip) ||
                line.startsWith("---\t/dev/null") ||
                (line.startsWith("@@") && line.endsWith("@@"))
    }

    private fun handleNewFile(lines: List<String>, index: Int): Pair<String, Int>? {
        val line = lines[index]
        if (!line.startsWith("new file mode")) return null

        val nextLine = lines.getOrNull(index + 1) ?: return null
        if (!nextLine.startsWith("--- /dev/null")) return null

        val nextNextLine = lines.getOrNull(index + 2) ?: return null
        val withoutHead = nextNextLine.substring("+++ b/".length)
        val tabIndex = withoutHead.indexOf("\t")
        val withoutFooter = if (tabIndex > 0) {
            withoutHead.substring(0, tabIndex)
        } else {
            withoutHead
        }

        return Pair("new file $withoutFooter", index + 3)
    }

    private fun handleRename(lines: List<String>, index: Int): Pair<String, Int>? {
        val line = lines[index]
        if (!line.startsWith("rename from")) return null

        val nextLine = lines.getOrNull(index + 1) ?: return null
        if (!nextLine.startsWith("rename to")) return null

        val from = line.substring("rename from ".length)
        val to = nextLine.substring("rename to ".length)

        return Pair("rename file from $from to $to", index + 4)
    }

    private fun handleImportChange(lines: List<String>, index: Int, length: Int): Pair<String, Int>? {
        val line = lines[index]
        if (!line.startsWith(" import")) return null

        val nextLine = lines.getOrNull(index + 1) ?: return null
        if (!nextLine.startsWith(" import")) return null

        var oldImportLine = ""
        var newImportLine = ""
        val importLines = ArrayList<String>()
        importLines.add(line)
        importLines.add(nextLine)

        var tryToFindIndex = index + 2
        while (tryToFindIndex < length) {
            val tryLine = lines[tryToFindIndex]
            when {
                tryLine.startsWith("Index:") -> break
                tryLine.startsWith(" import") -> importLines.add(tryLine)
                tryLine.startsWith("-import ") -> {
                    oldImportLine = tryLine.substring("-import ".length)
                    importLines.add(tryLine)
                }
                tryLine.startsWith("+import ") -> {
                    newImportLine = tryLine.substring("+import ".length)
                    importLines.add(tryLine)
                }
            }
            tryToFindIndex++
        }

        if (oldImportLine.isNotEmpty() && newImportLine.isNotEmpty()) {
            if (importLines.size == tryToFindIndex - index) {
                return Pair("change import from $oldImportLine to $newImportLine", tryToFindIndex)
            }
        }

        return null
    }

    private fun handleDelete(lines: List<String>, index: Int, length: Int): Pair<String, Int>? {
        val line = lines[index]
        if (!line.startsWith("deleted file mode")) return null

        val nextLine = lines.getOrNull(index + 1) ?: return null
        if (!nextLine.startsWith("--- a/")) return null

        val withoutHead = nextLine.substring("--- a/".length)
        val tabIndex = withoutHead.indexOf("\t")
        val withoutFooter = if (tabIndex > 0) {
            withoutHead.substring(0, tabIndex)
        } else {
            withoutHead
        }

        var newIndex = index + 2
        while (newIndex < length) {
            val nextNextLine = lines.getOrNull(newIndex) ?: break
            if (nextNextLine.startsWith("Index:")) {
                newIndex++
                break
            }
            newIndex++
        }

        return Pair("delete file $withoutFooter", newIndex)
    }

    private fun handleModifyMarkers(lines: List<String>, index: Int): Pair<String, Int>? {
        val line = lines[index]
        if (!line.startsWith("---") && !line.startsWith("+++")) return null

        val nextLine = lines.getOrNull(index + 1) ?: return null
        if (!nextLine.startsWith("+++")) return null

        val substringBefore = line.substringBefore("(revision")
        val startLine = substringBefore.substring("--- a/".length).trim()

        var endIndex = nextLine.indexOf("(date")
        if (endIndex == -1) endIndex = nextLine.indexOf("(revision")
        if (endIndex == -1) endIndex = nextLine.length

        val withoutEnd = nextLine.substring("+++ b/".length, endIndex).trim()

        if (startLine == withoutEnd) {
            return Pair("modify file $startLine", index + 2)
        }

        return null
    }
}

