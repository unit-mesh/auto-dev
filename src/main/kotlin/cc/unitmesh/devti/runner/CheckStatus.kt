package cc.unitmesh.devti.runner

enum class CheckStatus(val rawStatus: String) {
  Unchecked("UNCHECKED"),
  Solved("CORRECT"),
  Failed("WRONG");

  companion object {
    fun String.toCheckStatus(): CheckStatus = when (this) {
      "CORRECT" -> Solved
      "WRONG" -> Failed
      else -> Unchecked
    }
  }
}