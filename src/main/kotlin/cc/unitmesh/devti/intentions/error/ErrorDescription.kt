package cc.unitmesh.devti.intentions.error

import com.intellij.openapi.editor.Editor

class ErrorDescription(val text: String, val consoleLineFrom: Int, val consoleLineTo: Int, val editor: Editor)