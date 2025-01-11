package cc.unitmesh.devti.diff.model

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList

sealed class DiffLine {
    data class Same(val line: String) : DiffLine()
    data class New(val line: String) : DiffLine()
    data class Old(val line: String) : DiffLine()
}

fun streamDiff(oldLines: List<String>, newLines: Flow<String>): Flow<DiffLine> = flow {
    val newLinesList = newLines.toList()
    val diffs = diff(oldLines, newLinesList)
    for (diff in diffs) {
        emit(diff)
    }
}

private fun diff(oldLines: List<String>, newLines: List<String>): List<DiffLine> {
    // 计算 LCS 长度表
    val lcs = computeLcsTable(oldLines, newLines)
    // 回溯 LCS 表，构建差异列表
    val diffs = mutableListOf<DiffLine>()
    backtrackDiff(lcs, oldLines, newLines, oldLines.size, newLines.size, diffs)
    return diffs
}

private fun computeLcsTable(oldLines: List<String>, newLines: List<String>): Array<IntArray> {
    val m = oldLines.size
    val n = newLines.size
    val table = Array(m + 1) { IntArray(n + 1) }
    for (i in 0 until m) {
        for (j in 0 until n) {
            if (oldLines[i] == newLines[j]) {
                table[i + 1][j + 1] = table[i][j] + 1
            } else {
                table[i + 1][j + 1] = maxOf(table[i + 1][j], table[i][j + 1])
            }
        }
    }
    return table
}

private fun backtrackDiff(
    lcs: Array<IntArray>,
    oldLines: List<String>,
    newLines: List<String>,
    i: Int,
    j: Int,
    diffs: MutableList<DiffLine>,
) {
    if (i > 0 && j > 0 && oldLines[i - 1] == newLines[j - 1]) {
        backtrackDiff(lcs, oldLines, newLines, i - 1, j - 1, diffs)
        diffs.add(DiffLine.Same(oldLines[i - 1]))
    } else if (j > 0 && (i == 0 || lcs[i][j - 1] >= lcs[i - 1][j])) {
        backtrackDiff(lcs, oldLines, newLines, i, j - 1, diffs)
        diffs.add(DiffLine.New(newLines[j - 1]))
    } else if (i > 0 && (j == 0 || lcs[i][j - 1] < lcs[i - 1][j])) {
        backtrackDiff(lcs, oldLines, newLines, i - 1, j, diffs)
        diffs.add(DiffLine.Old(oldLines[i - 1]))
    }
}