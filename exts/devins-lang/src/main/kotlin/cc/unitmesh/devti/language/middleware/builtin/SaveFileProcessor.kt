package cc.unitmesh.devti.language.middleware.builtin

import cc.unitmesh.devti.language.ShireConstants
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import cc.unitmesh.devti.language.middleware.post.PostProcessorType
import cc.unitmesh.devti.language.middleware.post.PostProcessorContext
import cc.unitmesh.devti.language.middleware.post.PostProcessor
import cc.unitmesh.devti.util.parser.CodeFence

class SaveFileProcessor : PostProcessor, Disposable {
    override val processorName: String = PostProcessorType.SaveFile.handleName
    override val description: String = "`saveFile` will save the content / llm response to the file"

    override fun isApplicable(context: PostProcessorContext): Boolean = true

    override fun execute(
        project: Project,
        context: PostProcessorContext,
        console: ConsoleView?,
        args: List<Any>,
    ): String {
        val fileName: String
        val ext = getFileExt(context)
        if (args.isNotEmpty()) {
            fileName = getValidFilePath(args[0].toString(), ext)
            handleForProjectFile(project, fileName, context, console)
        } else {
            fileName = "${System.currentTimeMillis()}.$ext"
            handleForTempFile(project, fileName, context, console)
        }

        return fileName
    }

    private fun getFileExt(context: PostProcessorContext): String {
        val language = context.genTargetLanguage ?: PlainTextLanguage.INSTANCE
        return context.genTargetExtension ?: language?.associatedFileType?.defaultExtension ?: "txt"
    }

    private fun handleForTempFile(
        project: Project,
        fileName: String,
        context: PostProcessorContext,
        console: ConsoleView?,
    ) {
        ApplicationManager.getApplication().invokeAndWait {
            WriteAction.compute<VirtualFile, Throwable> {
                val outputDir = ShireConstants.outputDir(project)

                val outputFile = outputDir?.createChildData(this, fileName)
                    ?: throw IllegalStateException("Failed to save file")

                val content = getContent(context)
                outputFile.setBinaryContent(content?.toByteArray() ?: ByteArray(0))
                context.pipeData["output"] = outputFile

                project.guessProjectDir()?.refresh(true, true)

                console?.print("Saved to ${outputFile.canonicalPath}\n", ConsoleViewContentType.SYSTEM_OUTPUT)
                outputFile
            }
        }
    }

    private fun handleForProjectFile(
        project: Project,
        filepath: String,
        context: PostProcessorContext,
        console: ConsoleView?
    ) {
        var fileName = filepath
        ApplicationManager.getApplication().invokeAndWait {
            WriteAction.compute<VirtualFile, Throwable> {
                val projectDir = project.guessProjectDir()
                // if filename starts with / means it's an absolute path, we need to get relative path
                if (fileName.startsWith("/")) {
                    val projectPath = projectDir?.canonicalPath
                    if (projectPath != null) {
                        fileName = fileName.replace(projectPath, "")
                    }
                }

                // filename may include path, like: `src/main/java/HelloWorld.java`, we need to split it
                // first check if the file is already in the project
                var outputFile = projectDir?.findFileByRelativePath(fileName)
                if (outputFile == null) {
                    outputFile = createFile(fileName, projectDir)
                }

                val content = getContent(context)
                outputFile!!.setBinaryContent(content?.toByteArray() ?: ByteArray(0))
                context.pipeData["output"] = outputFile

                projectDir?.refresh(true, true)

                console?.print("Saved to ${outputFile.canonicalPath}", ConsoleViewContentType.SYSTEM_OUTPUT)
                outputFile
            }
        }
    }

    private fun getContent(context: PostProcessorContext): String? {
        val outputData = context.pipeData["output"]

        if (outputData is String && outputData.isNotEmpty()) {
            return outputData
        }

        if (context.lastTaskOutput?.isNotEmpty() == true) {
            return context.lastTaskOutput
        }

        return context.genText
    }

    private fun createFile(
        fileName: String,
        projectDir: VirtualFile?,
    ): VirtualFile {
        val path = fileName.split("/").dropLast(1)
        val name = fileName.split("/").last()

        var parentDir = projectDir

        // create directories if not exist
        for (dir in path) {
            parentDir = parentDir?.findChild(dir) ?: parentDir?.createChildDirectory(this, dir)
        }

        val outputFile = parentDir?.createChildData(this, name)
            ?: throw IllegalStateException("Failed to save file")

        return outputFile
    }

    override fun dispose() {
        Disposer.dispose(this)
    }
}

/**
 * 根据给定的文件路径和扩展名，返回一个有效的文件路径。
 *
 * <pre>
 *    {@code
 *        String validPath = getValidFilePath("example.txt", "txt");
 *        // validPath = "example.txt"
 *
 *        String validPath2 = getValidFilePath("", "txt");
 *        // validPath2 = "1633024800000.txt" (当前时间戳)
 *    }
 *    </pre>
 *
 * @param filePath 文件路径，可以为空或包含特殊字符
 * @param ext 文件扩展名，用于在文件路径无效时生成默认文件名
 * @return 有效的文件路径，如果输入路径无效，则返回基于时间戳的默认文件名
 */
fun getValidFilePath(filePath: String, ext: String): String {
    val pathRegex = """^([a-zA-Z]:\\|\\\\|/|)([a-zA-Z0-9_\-\\/.]+)$""".toRegex()

    if (filePath.isBlank()) {
        return "${System.currentTimeMillis()}.$ext"
    }

    return if (pathRegex.matches(filePath)) {
        filePath
    } else if (filePath.contains("`") && filePath.contains("```")) {
        CodeFence.parse(filePath).text
    } else {
        "${System.currentTimeMillis()}.$ext"
    }
}