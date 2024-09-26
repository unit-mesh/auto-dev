// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cc.unitmesh.devti.runner

enum class RunnerStatus(val rawStatus: String) {
  Unchecked("UNCHECKED"),
  Solved("CORRECT"),
  Failed("WRONG");

  companion object {
    fun String.toCheckStatus(): RunnerStatus = when (this) {
      "CORRECT" -> Solved
      "WRONG" -> Failed
      else -> Unchecked
    }
  }
}