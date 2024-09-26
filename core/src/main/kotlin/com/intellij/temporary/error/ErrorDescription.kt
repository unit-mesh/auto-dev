// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.temporary.error

import com.intellij.openapi.editor.Editor

data class ErrorDescription(val text: String, val consoleLineFrom: Int, val consoleLineTo: Int, val editor: Editor)