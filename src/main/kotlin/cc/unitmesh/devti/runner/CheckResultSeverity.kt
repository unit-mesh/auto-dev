package cc.unitmesh.devti.runner

enum class CheckResultSeverity {
  Info, Warning, Error;

  fun isWaring() = this == Warning
  fun isInfo() = this == Info
}