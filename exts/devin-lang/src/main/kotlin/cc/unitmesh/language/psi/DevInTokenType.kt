package cc.unitmesh.language.psi

import com.intellij.psi.tree.IElementType
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.NotNull

class DevInTokenType(
    @NotNull @NonNls debugName: String
): IElementType(debugName, cc.unitmesh.language.DevInLanguage.INSTANCE) {
    override fun toString(): String {
        return "DevInTokenType." + super.toString()
    }
}