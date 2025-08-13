package cc.unitmesh.git.actions.vcs

import cc.unitmesh.devti.context.ClassContext
import cc.unitmesh.devti.context.ClassContextProvider
import cc.unitmesh.devti.context.FileContextProvider
import cc.unitmesh.devti.context.MethodContextProvider
import cc.unitmesh.devti.context.VariableContextProvider
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiNamedElement

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
            mermaidBuilder.toString()
        } else {
            "classDiagram\n    class NoChanges {\n        +No structural changes detected\n    }"
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
        return fileContext?.classes?.map { psiElement ->
            ClassContextProvider(true).from(psiElement)
        } ?: emptyList()
    }

    private fun generateClassDiagramChanges(
        builder: StringBuilder,
        fileName: String,
        beforeStructure: List<ClassContext>,
        afterStructure: List<ClassContext>
    ) {
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
                        generateClassDefinition(builder, afterClass, "+")
                    }

                    beforeClass != null && afterClass == null -> {
                        generateClassDefinition(builder, beforeClass, "-")
                    }

                    beforeClass != null && afterClass != null -> {
                        val changes = analyzeClassChanges(beforeClass, afterClass)
                        if (changes.hasStructuralChanges()) {
                            generateModifiedClassDefinition(builder, beforeClass, afterClass, changes)
                        } else {
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
            if (methodName == className) return@forEach

            if (methodName.isNotEmpty()) {
                builder.appendLine("        -$methodName")
            }
        }

        // 显示保持的方法
        beforeMethods.intersect(afterMethods).forEach { methodName ->
            if (methodName == className) return@forEach

            if (methodName.isNotEmpty()) {
                builder.appendLine("        $methodName")
            }
        }

        // 显示新增的方法
        afterMethods.subtract(beforeMethods).forEach { methodName ->
            if (methodName == className) return@forEach

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

    private fun extractMethodName(methodElement: PsiElement): String {
        val returnType = MethodContextProvider(false, false).from(methodElement).returnType
        if (methodElement is PsiNamedElement && methodElement.name != null) {
            if (returnType != "null" &&  returnType != null) {
                return returnType + " " + methodElement.name!!
            }

            return methodElement.name!!
        }

        return "unknown"
    }

    /**
     * 从 PsiElement 提取字段名
     */
    private fun extractFieldName(fieldElement: PsiElement): String {
        val returnType = VariableContextProvider(false, false, false).from(fieldElement).type
        if (fieldElement is PsiNamedElement && returnType != null) {
            return returnType + " " + fieldElement.name!!
        }

        if (fieldElement is PsiNamedElement && fieldElement.name != null) {
            return fieldElement.name!!
        }

        return "unknown"
    }

    /**
     * 分析类的详细变化
     */
    private fun analyzeClassChanges(beforeClass: ClassContext, afterClass: ClassContext): ClassChanges {
        val beforeMethods = beforeClass.methods.mapNotNull {
            MethodContextProvider(false, gatherUsages = false).from(it)?.signature
        }.toSet()
        val afterMethods = afterClass.methods.mapNotNull {
            MethodContextProvider(false, gatherUsages = false).from(it)?.signature
        }.toSet()

        val beforeFields = beforeClass.fields.mapNotNull {
            VariableContextProvider(false, false, false).from(it)?.shortFormat()
        }.toSet()
        val afterFields = afterClass.fields.mapNotNull {
            VariableContextProvider(false, false, false).from(it)?.shortFormat()
        }.toSet()

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
