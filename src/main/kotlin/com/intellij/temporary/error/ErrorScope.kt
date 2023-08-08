// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.temporary.error

import com.intellij.openapi.vfs.VirtualFile

class ErrorScope(val lineStart: Int, val lineFinish: Int, val text: String, val thisVirtualFile: VirtualFile) {
    fun containsLineNumber(lineNumber: Int, virtualFile: VirtualFile): Boolean {
        return lineNumber in lineStart..lineFinish && thisVirtualFile == virtualFile
    }
}