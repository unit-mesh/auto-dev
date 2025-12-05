package cc.unitmesh.devti.language.lexer

import cc.unitmesh.devti.language.DevInLanguage
import com.intellij.psi.tree.IElementType
import org.jetbrains.annotations.NonNls

class DevInTokenType(@NonNls debugName: String) : IElementType(debugName, DevInLanguage) {
    override fun toString(): String = "DevInTokenType." + super.toString()
}