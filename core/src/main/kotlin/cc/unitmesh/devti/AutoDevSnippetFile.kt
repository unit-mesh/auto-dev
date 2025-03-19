// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cc.unitmesh.devti

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile

const val AUTODEV_SNIPPET_NAME = "autodev-snippet-"

object AutoDevSnippetFile {
    fun isSnippet(file: VirtualFile): Boolean {
        if (file !is LightVirtualFile) return false
        return file.name.startsWith("autodev-snippet-") || file.name.startsWith("DevIn-") || file.name.startsWith("devIn-")
    }

    /**
     * extension should not contain dot
     */
    fun naming(extension: String): String {
        val time = System.currentTimeMillis()
        return "$AUTODEV_SNIPPET_NAME$time.$extension"
    }
}