package cc.unitmesh.devti.actions

import cc.unitmesh.devti.AutoDevBundle
import com.intellij.ide.actions.CreateDirectoryCompletionContributor
import com.intellij.psi.PsiDirectory
import org.jetbrains.annotations.Nls
import org.jetbrains.jps.model.java.JavaResourceRootType
import java.io.File

class AutoDevDirectoryCompletionContributor : CreateDirectoryCompletionContributor {

    override fun getDescription(): @Nls(capitalization = Nls.Capitalization.Sentence) String {
        return AutoDevBundle.message("autodev.directory.completion.description")
    }

    override fun getVariants(psiDirectory: PsiDirectory): Collection<CreateDirectoryCompletionContributor.Variant> {
        val result = mutableListOf<CreateDirectoryCompletionContributor.Variant>()
        // prompt
        result += CreateDirectoryCompletionContributor.Variant("prompts", JavaResourceRootType.RESOURCE)
        // prompts + File.separator + "quick"
        result += CreateDirectoryCompletionContributor.Variant(
            "prompts" + File.separator + "quick",
            JavaResourceRootType.RESOURCE
        )
        // prompts + File.separator + "templates
        result += CreateDirectoryCompletionContributor.Variant(
            "prompts" + File.separator + "templates",
            JavaResourceRootType.RESOURCE
        )

        return result.toList()
    }
}

