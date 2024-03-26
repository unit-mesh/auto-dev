package cc.unitmesh.devti.runner

import org.jetbrains.annotations.Nls

class CheckResult(
    val status: CheckStatus,
    @Nls(capitalization = Nls.Capitalization.Sentence) val message: String = "",
    val details: String? = null,
    val diff: CheckResultDiff? = null,
    val severity: CheckResultSeverity = CheckResultSeverity.Info,
    val hyperlinkAction: (() -> Unit)? = null,
) {
    val fullMessage: String get() = if (details == null) message else "$message\n\n$details"
    val isSolved: Boolean get() = status == CheckStatus.Solved

    companion object {
        val NO_LOCAL_CHECK = CheckResult(CheckStatus.Unchecked, "check.result.local.check.unavailable")
        val CONNECTION_FAILED = CheckResult(CheckStatus.Unchecked, "check.result.connection.failed")
        val SOLVED = CheckResult(CheckStatus.Solved)
        val CANCELED = CheckResult(CheckStatus.Unchecked, "check.result.canceled")
        val UNCHECKED = CheckResult(CheckStatus.Unchecked)

        val noTestsRun: CheckResult
            get() = CheckResult(
                CheckStatus.Unchecked,
                "check.no.tests.with.help.guide",
            )

        val failedToCheck: CheckResult
            get() = CheckResult(
                CheckStatus.Unchecked,
                "error.failed.to.launch.checking.with.help.guide",
            )

    }

}

data class CheckResultDiff(val expected: String, val actual: String, val title: String = "")
