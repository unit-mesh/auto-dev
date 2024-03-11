package cc.unitmesh.language.psi

import cc.unitmesh.language.DevInLanguage
import com.intellij.psi.tree.IElementType

class DevInElementType(debugName: String): IElementType(debugName, DevInLanguage.INSTANCE)