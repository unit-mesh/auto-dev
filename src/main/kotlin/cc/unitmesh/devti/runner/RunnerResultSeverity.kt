// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cc.unitmesh.devti.runner

enum class RunnerResultSeverity {
  Info, Warning, Error;

  fun isWaring() = this == Warning
  fun isInfo() = this == Info
}