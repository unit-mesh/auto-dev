// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.temporary.gui.block

import com.intellij.lang.Language
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.testFramework.LightVirtualFile
import javax.swing.JComponent

class CodePartEditorInfo(
    @JvmField val code: GraphProperty<String>,
    @JvmField val component: JComponent,
    @JvmField val editor: EditorEx,
    private val file: LightVirtualFile
) {
    var language: Language
        get() = file.language
        set(value) {
            file.language = value
        }
}
