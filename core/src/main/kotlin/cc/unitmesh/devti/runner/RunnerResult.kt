// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cc.unitmesh.devti.runner

import org.jetbrains.annotations.Nls

class RunnerResult(
    val status: RunnerStatus,
    @Nls(capitalization = Nls.Capitalization.Sentence) val message: String = "",
    val details: String? = null,
    val diff: CheckResultDiff? = null,
    /**
     * for example, when lost Node.js package.json
     */
    val severity: RunnerResultSeverity = RunnerResultSeverity.Info,
    /**
     * Like sync Gradle or others
     */
    val hyperlinkAction: (() -> Unit)? = null,
) {
    val fullMessage: String get() = if (details == null) message else "$message\n\n$details"
    val isSolved: Boolean get() = status == RunnerStatus.Solved

    companion object {
        val NO_LOCAL_CHECK = RunnerResult(RunnerStatus.Unchecked, "check.result.local.check.unavailable")
        val CONNECTION_FAILED = RunnerResult(RunnerStatus.Unchecked, "check.result.connection.failed")
        val SOLVED = RunnerResult(RunnerStatus.Solved)
        val CANCELED = RunnerResult(RunnerStatus.Unchecked, "check.result.canceled")
        val UNCHECKED = RunnerResult(RunnerStatus.Unchecked)

        val noTestsRun: RunnerResult
            get() = RunnerResult(
                RunnerStatus.Unchecked,
                "check.no.tests.with.help.guide",
            )

        val failedToCheck: RunnerResult
            get() = RunnerResult(
                RunnerStatus.Unchecked,
                "error.failed.to.launch.checking.with.help.guide",
            )

    }

}

data class CheckResultDiff(val expected: String, val actual: String, val title: String = "")
