package cc.unitmesh.rust.provider

import cc.unitmesh.devti.context.builder.CodeModifier
import com.intellij.lang.Language
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.rust.lang.RsLanguage

class RustCodeModifier : CodeModifier {
    override fun isApplicable(language: Language): Boolean = language is RsLanguage

    override fun insertTestCode(sourceFile: VirtualFile, project: Project, code: String): Boolean {
        // in Rust, lang the test code is inserted into the source file

        return true
    }

    override fun insertMethod(sourceFile: VirtualFile, project: Project, code: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun insertClass(sourceFile: VirtualFile, project: Project, code: String): Boolean {
        TODO("Not yet implemented")
    }
}
