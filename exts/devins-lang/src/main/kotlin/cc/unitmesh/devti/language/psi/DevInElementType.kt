package cc.unitmesh.devti.language.psi

import cc.unitmesh.devti.language.DevInLanguage
import com.intellij.psi.tree.IElementType

class DevInElementType(debugName: String): IElementType(debugName, DevInLanguage.INSTANCE)