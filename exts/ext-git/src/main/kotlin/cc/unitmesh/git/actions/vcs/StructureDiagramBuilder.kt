package cc.unitmesh.git.actions.vcs

import cc.unitmesh.devti.context.ClassContext
import cc.unitmesh.devti.context.ClassContextProvider
import cc.unitmesh.devti.context.FileContextProvider
import com.intellij.lang.Language
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory

/**
 * 获取变更前后的代码结构变化，以生成 Mermaid 变化图
 */
class StructureDiagramBuilder(val project: Project, val changes: List<Change>) {
    /**
     * 构建代码结构变化图
     *
     * 功能描述：
     * 1. 收集变更前文件的结构信息
     * 2. 收集变更后文件的结构信息
     * 3. 对比前后结构差异
     * 4. 生成 Mermaid 格式的变化图
     *
     * 变化图符号说明：
     * - "+" 表示新增的方法/类/字段
     * - "-" 表示删除的方法/类/字段
     * - "~" 表示修改的方法/类/字段
     *
     * 返回：Mermaid 格式的字符串，可直接用于渲染结构变化图
     */
    fun build(): String {
        val mermaidBuilder = StringBuilder()
        mermaidBuilder.appendLine("classDiagram")

        changes.forEach { change ->
            val beforeStructure = getStructureFromRevision(change.beforeRevision)
            val afterStructure = getStructureFromRevision(change.afterRevision)

            val fileName = change.afterRevision?.file?.name
                ?: change.beforeRevision?.file?.name
                ?: "unknown"

            // 生成类图变化
            generateClassDiagramChanges(mermaidBuilder, fileName, beforeStructure, afterStructure)
        }

        return if (mermaidBuilder.length > "classDiagram\n".length) {
            "\n```mermaid\n$mermaidBuilder\n```\n"
        } else {
            "\n```mermaid\nclassDiagram\n    class NoChanges {\n        +No structural changes detected\n    }\n```\n"
        }
    }

    /**
     * 从 ContentRevision 获取结构信息
     */
    private fun getStructureFromRevision(revision: com.intellij.openapi.vcs.changes.ContentRevision?): List<ClassContext> {
        if (revision == null) return emptyList()

        val content = runReadAction { revision.content } ?: return emptyList()
        val tempFileName = revision.file.name
        val psiFile = runReadAction {
            val factory = PsiFileFactory.getInstance(project)
            val fileType = FileTypeManager.getInstance().getFileTypeByFileName(tempFileName)
            factory.createFileFromText("temp.${fileType.defaultExtension}", fileType, content)
        }

        val fileContext = FileContextProvider().from(psiFile)
        return fileContext?.classes?.mapNotNull { psiElement ->
            val context = ClassContextProvider(true).from(psiElement)
            if (context.name == null) null else context
        } ?: emptyList()
    }

    private fun generateClassDiagramChanges(
        builder: StringBuilder,
        fileName: String,
        beforeStructure: List<ClassContext>,
        afterStructure: List<ClassContext>
    ) {
        // 创建类名到上下文的映射
        val beforeClasses = beforeStructure.associateBy { it.name }
        val afterClasses = afterStructure.associateBy { it.name }

        // 处理所有类的变化
        val allClassNames = (beforeClasses.keys + afterClasses.keys).distinct()

        allClassNames.forEach { className ->
            if (className != null) {
                val beforeClass = beforeClasses[className]
                val afterClass = afterClasses[className]

                when {
                    beforeClass == null && afterClass != null -> {
                        // 新增的类
                        generateClassDefinition(builder, afterClass, "+")
                    }

                    beforeClass != null && afterClass == null -> {
                        // 删除的类
                        generateClassDefinition(builder, beforeClass, "-")
                    }

                    beforeClass != null && afterClass != null -> {
                        // 检查是否有结构变化
                        val changes = analyzeClassChanges(beforeClass, afterClass)
                        if (changes.hasStructuralChanges()) {
                            generateModifiedClassDefinition(builder, beforeClass, afterClass, changes)
                        } else {
                            // 没有结构变化，显示当前状态
                            generateClassDefinition(builder, afterClass, "")
                        }
                    }
                }
            }
        }
    }

    /**
     * 生成 Class Diagram 格式的类定义
     */
    private fun generateClassDefinition(
        builder: StringBuilder,
        classContext: ClassContext,
        changePrefix: String
    ) {
        val className = classContext.name ?: return
        val sanitizedClassName = sanitizeClassName(className)

        builder.appendLine("    class $sanitizedClassName {")

        // 生成字段
        classContext.fields.forEach { field ->
            val fieldName = extractFieldName(field)
            if (fieldName.isNotEmpty()) {
                builder.appendLine("        $changePrefix$fieldName")
            }
        }

        // 生成方法
        classContext.methods.forEach { method ->
            val methodName = extractMethodName(method)
            if (methodName.isNotEmpty()) {
                builder.appendLine("        $changePrefix$methodName")
            }
        }

        builder.appendLine("    }")

        // 如果有变化标记，添加注释
        if (changePrefix.isNotEmpty()) {
            val changeType = when (changePrefix) {
                "+" -> "Added"
                "-" -> "Removed"
                "~" -> "Modified"
                else -> "Changed"
            }
            builder.appendLine("    $sanitizedClassName : $changeType")
        }
    }

    /**
     * 生成修改后的类定义，显示变化
     */
    private fun generateModifiedClassDefinition(
        builder: StringBuilder,
        beforeClass: ClassContext,
        afterClass: ClassContext,
        changes: ClassChanges
    ) {
        val className = afterClass.name ?: return
        val sanitizedClassName = sanitizeClassName(className)

        builder.appendLine("    class $sanitizedClassName {")

        // 生成字段变化
        val beforeFields = beforeClass.fields.map { extractFieldName(it) }.toSet()
        val afterFields = afterClass.fields.map { extractFieldName(it) }.toSet()

        // 显示删除的字段
        beforeFields.subtract(afterFields).forEach { fieldName ->
            if (fieldName.isNotEmpty()) {
                builder.appendLine("        -$fieldName")
            }
        }

        // 显示保持的字段
        beforeFields.intersect(afterFields).forEach { fieldName ->
            if (fieldName.isNotEmpty()) {
                builder.appendLine("        $fieldName")
            }
        }

        // 显示新增的字段
        afterFields.subtract(beforeFields).forEach { fieldName ->
            if (fieldName.isNotEmpty()) {
                builder.appendLine("        +$fieldName")
            }
        }

        // 生成方法变化
        val beforeMethods = beforeClass.methods.map { extractMethodName(it) }.toSet()
        val afterMethods = afterClass.methods.map { extractMethodName(it) }.toSet()

        // 显示删除的方法
        beforeMethods.subtract(afterMethods).forEach { methodName ->
            if (methodName.isNotEmpty()) {
                builder.appendLine("        -$methodName")
            }
        }

        // 显示保持的方法
        beforeMethods.intersect(afterMethods).forEach { methodName ->
            if (methodName.isNotEmpty()) {
                builder.appendLine("        $methodName")
            }
        }

        // 显示新增的方法
        afterMethods.subtract(beforeMethods).forEach { methodName ->
            if (methodName.isNotEmpty()) {
                builder.appendLine("        +$methodName")
            }
        }

        builder.appendLine("    }")
        builder.appendLine("    $sanitizedClassName : Modified")
    }

    /**
     * 清理类名，确保符合 Mermaid Class Diagram 语法
     */
    private fun sanitizeClassName(className: String): String {
        return className.replace(Regex("[^a-zA-Z0-9_]"), "_")
    }

    /**
     * 检查类是否发生了变化
     */
    private fun hasClassChanged(beforeClass: ClassContext, afterClass: ClassContext): Boolean {
        // 比较方法签名（更精确的比较）
        val beforeMethods = beforeClass.methods.mapNotNull { extractMethodSignature(it) }.toSet()
        val afterMethods = afterClass.methods.mapNotNull { extractMethodSignature(it) }.toSet()

        // 比较字段签名
        val beforeFields = beforeClass.fields.mapNotNull { extractFieldSignature(it) }.toSet()
        val afterFields = afterClass.fields.mapNotNull { extractFieldSignature(it) }.toSet()

        return beforeMethods != afterMethods || beforeFields != afterFields
    }

    /**
     * 从 PsiElement 提取方法名
     */
    private fun extractMethodName(methodElement: PsiElement): String {
        return extractMethodSignature(methodElement)?.substringBefore("(") ?: "unknown"
    }

    /**
     * 从 PsiElement 提取方法签名（更精确）
     */
    private fun extractMethodSignature(methodElement: PsiElement): String? {
        return runReadAction {
            try {
                val text = methodElement.text ?: return@runReadAction null
                val lines = text.lines().filter { it.trim().isNotEmpty() }

                // 查找方法声明行（包含括号的行）
                val methodLine = lines.find { line ->
                    val trimmed = line.trim()
                    // 排除注释和注解
                    !trimmed.startsWith("//") &&
                            !trimmed.startsWith("/*") &&
                            !trimmed.startsWith("*") &&
                            !trimmed.startsWith("@") &&
                            trimmed.contains("(") &&
                            !trimmed.startsWith("if") &&
                            !trimmed.startsWith("while") &&
                            !trimmed.startsWith("for")
                } ?: return@runReadAction null

                val trimmed = methodLine.trim()
                when {
                    trimmed.contains("fun ") -> {
                        // Kotlin 方法
                        val funPart = trimmed.substringAfter("fun ").substringBefore("{").trim()
                        if (funPart.contains("(")) funPart else null
                    }

                    trimmed.contains("def ") -> {
                        // Python 方法
                        val defPart = trimmed.substringAfter("def ").substringBefore(":").trim()
                        if (defPart.contains("(")) defPart else null
                    }

                    trimmed.contains("function ") -> {
                        // JavaScript 方法
                        val funcPart = trimmed.substringAfter("function ").substringBefore("{").trim()
                        if (funcPart.contains("(")) funcPart else null
                    }

                    trimmed.contains("(") -> {
                        // Java/C# 等方法
                        val beforeBrace = trimmed.substringBefore("{").trim()
                        val beforeParen = beforeBrace.substringBefore("(")
                        val afterParen = beforeBrace.substringAfter("(").substringBefore(")")
                        val methodName = beforeParen.split(" ").lastOrNull()?.trim()
                        if (methodName != null) "$methodName($afterParen)" else null
                    }

                    else -> null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * 从 PsiElement 提取字段名
     */
    private fun extractFieldName(fieldElement: PsiElement): String {
        return extractFieldSignature(fieldElement)?.substringAfterLast(" ")?.substringBefore(";")?.substringBefore("=")
            ?: "unknown"
    }

    /**
     * 从 PsiElement 提取字段签名（更精确）
     */
    private fun extractFieldSignature(fieldElement: PsiElement): String? {
        return runReadAction {
            try {
                val text = fieldElement.text ?: return@runReadAction null
                val lines = text.lines().filter { it.trim().isNotEmpty() }

                // 查找字段声明行
                val fieldLine = lines.find { line ->
                    val trimmed = line.trim()
                    // 排除注释、注解和方法
                    !trimmed.startsWith("//") &&
                            !trimmed.startsWith("/*") &&
                            !trimmed.startsWith("*") &&
                            !trimmed.startsWith("@") &&
                            !trimmed.contains("(") &&
                            (trimmed.contains("private ") || trimmed.contains("public ") ||
                                    trimmed.contains("protected ") || trimmed.contains("val ") ||
                                    trimmed.contains("var ") || trimmed.contains("final ") ||
                                    trimmed.contains("static "))
                } ?: return@runReadAction null

                val trimmed = fieldLine.trim().substringBefore(";").substringBefore("=")
                when {
                    trimmed.contains("val ") -> {
                        // Kotlin val
                        val valPart = trimmed.substringAfter("val ").trim()
                        if (valPart.contains(":")) {
                            val name = valPart.substringBefore(":").trim()
                            val type = valPart.substringAfter(":").trim()
                            "$type $name"
                        } else {
                            "val $valPart"
                        }
                    }

                    trimmed.contains("var ") -> {
                        // Kotlin var
                        val varPart = trimmed.substringAfter("var ").trim()
                        if (varPart.contains(":")) {
                            val name = varPart.substringBefore(":").trim()
                            val type = varPart.substringAfter(":").trim()
                            "$type $name"
                        } else {
                            "var $varPart"
                        }
                    }

                    else -> {
                        // Java/C# 等字段
                        trimmed
                    }
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * 分析类的详细变化
     */
    private fun analyzeClassChanges(beforeClass: ClassContext, afterClass: ClassContext): ClassChanges {
        val beforeMethods = beforeClass.methods.mapNotNull { extractMethodSignature(it) }.toSet()
        val afterMethods = afterClass.methods.mapNotNull { extractMethodSignature(it) }.toSet()

        val beforeFields = beforeClass.fields.mapNotNull { extractFieldSignature(it) }.toSet()
        val afterFields = afterClass.fields.mapNotNull { extractFieldSignature(it) }.toSet()

        return ClassChanges(
            addedMethods = afterMethods - beforeMethods,
            removedMethods = beforeMethods - afterMethods,
            addedFields = afterFields - beforeFields,
            removedFields = beforeFields - afterFields
        )
    }
}

/**
 * 类变化的详细信息
 */
data class ClassChanges(
    val addedMethods: Set<String>,
    val removedMethods: Set<String>,
    val addedFields: Set<String>,
    val removedFields: Set<String>
) {
    fun hasStructuralChanges(): Boolean {
        return addedMethods.isNotEmpty() || removedMethods.isNotEmpty() ||
                addedFields.isNotEmpty() || removedFields.isNotEmpty()
    }
}
