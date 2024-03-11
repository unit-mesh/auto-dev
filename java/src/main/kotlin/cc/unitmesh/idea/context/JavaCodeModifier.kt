package cc.unitmesh.idea.context

import cc.unitmesh.devti.context.builder.CodeModifier
import cc.unitmesh.idea.service.JavaWriteTestService
import com.intellij.lang.Language
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*

open class JavaCodeModifier : CodeModifier {
    companion object {
        val log = logger<JavaWriteTestService>()
    }

    override fun isApplicable(language: Language): Boolean {
        return language is JavaLanguage
    }

    fun lookupFile(
        project: Project,
        sourceFile: VirtualFile,
    ) = PsiManager.getInstance(project).findFile(sourceFile) as PsiJavaFile

    override fun insertTestCode(sourceFile: VirtualFile, project: Project, code: String): Boolean {
        // remove surrounding ``` markdown code block
        val trimCode = code.trim().removeSurrounding("```").removePrefix("java")

        if (!trimCode.contains("@Test")) {
            log.warn("methodCode does not contain @Test annotation: $trimCode")
            insertMethod(sourceFile, project, trimCode)
            return false
        }

        val isClassStarted = trimCode.startsWith("import") || trimCode.startsWith("package")
        val isFullCode = isClassStarted && trimCode.contains("class ")
        // check is sourceFile has class
        val classes = runReadAction { lookupFile(project, sourceFile).classes }

        if (classes.isNotEmpty()) {
            val lastClass = classes.last()
            val classEndOffset = lastClass.textRange.endOffset

            val newCode = try {
                runReadAction {
                    val createFileFromText =
                        PsiFileFactory.getInstance(project)
                            .createFileFromText("Test.java", JavaLanguage.INSTANCE, trimCode)

                    createFileFromText?.text ?: trimCode
                }
            } catch (e: Throwable) {
                log.warn("Failed to create file from text: $trimCode", e)
                trimCode
            }

            WriteCommandAction.runWriteCommandAction(project) {
                val document = PsiDocumentManager.getInstance(project).getDocument(lastClass.containingFile)
                document?.replaceString(0, classEndOffset, newCode)
            }

            return true
        }

        if (isFullCode) {
            return insertClass(sourceFile, project, trimCode)
        }

        insertMethod(sourceFile, project, trimCode)
        return true
    }

    override fun insertMethod(sourceFile: VirtualFile, project: Project, code: String): Boolean {
        val rootElement = runReadAction {
            val psiJavaFile = lookupFile(project, sourceFile)
            val psiClass = psiJavaFile.classes.firstOrNull()
            if (psiClass == null) {
                log.error("Failed to find PsiClass in the source file: $psiJavaFile, code: $code")
                return@runReadAction null
            }

            return@runReadAction psiClass
        } ?: return false

        val newTestMethod = ReadAction.compute<PsiMethod, Throwable> {
            val psiElementFactory = PsiElementFactory.getInstance(project)
            try {
                val methodCode = psiElementFactory.createMethodFromText(code, rootElement)
                if (rootElement.findMethodsByName(methodCode.name, false).isNotEmpty()) {
                    log.error("Method already exists in the class: ${methodCode.name}")
                }

                methodCode
            } catch (e: Throwable) {
                log.error("Failed to create method from text: $code", e)
                return@compute null
            }
        }

        WriteCommandAction.runWriteCommandAction(project) {
            try {
                rootElement.add(newTestMethod)
            } catch (e: Throwable) {
                val classEndOffset = rootElement.textRange.endOffset
                val document = PsiDocumentManager.getInstance(project).getDocument(rootElement.containingFile)
                document?.insertString(classEndOffset - 1, "\n    ")
                document?.insertString(classEndOffset - 1 + "\n    ".length, newTestMethod.text)
            }
        }

        project.guessProjectDir()?.refresh(true, true)

        return true
    }

    override fun insertClass(sourceFile: VirtualFile, project: Project, code: String): Boolean {
        return WriteCommandAction.runWriteCommandAction<Boolean>(project) {
            val psiFile = lookupFile(project, sourceFile)
            val document = psiFile.viewProvider.document!!
            document.insertString(document.textLength, code)

            true
        }
    }
}
