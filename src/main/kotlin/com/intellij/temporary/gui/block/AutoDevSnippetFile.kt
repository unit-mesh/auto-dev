// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.temporary.gui.block

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile

const val AUTODEV_SNIPPET_NAME = "AutoDevSnippet"

object AutoDevSnippetFile {
    fun isSnippet(file: VirtualFile): Boolean {
        if (file is LightVirtualFile) {
            return file.getName() == AUTODEV_SNIPPET_NAME
        }

        return false
    }
}