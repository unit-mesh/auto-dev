// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.temporary.gui.block

import java.awt.Component

interface MessageBlockView {
    fun getBlock(): MessageBlock

    fun getComponent(): Component?

    fun initialize() {}
}