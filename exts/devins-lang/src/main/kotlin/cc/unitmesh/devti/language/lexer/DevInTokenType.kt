package cc.unitmesh.devti.language.lexer

import cc.unitmesh.devti.language.DevInLanguage
import com.intellij.psi.tree.IElementType
import org.jetbrains.annotations.NonNls

class DevInTokenType(debugName: @NonNls String) : IElementType(debugName, DevInLanguage) {
    override fun toString(): String = "DevInTokenType." + super.toString()
}