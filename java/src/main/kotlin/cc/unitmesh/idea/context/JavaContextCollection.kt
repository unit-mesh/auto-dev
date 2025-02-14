package cc.unitmesh.idea.context

import cc.unitmesh.devti.context.SimpleClassStructure
import com.intellij.openapi.diagnostic.logger
import com.intellij.psi.*
import com.intellij.psi.impl.source.PsiClassReferenceType

object JavaContextCollection {
    private val logger = logger<JavaContextCollection>()

    /**
     * This method takes a PsiClass object as input and builds a tree of the class and its fields, including the fields of the fields, and so on. The resulting tree is represented as a HashMap where the keys are the PsiClass objects and the values are ArrayLists of PsiField objects.
     *
     * @param clazz the PsiClass object for which the tree needs to be built
     * @return a HashMap where the keys are the PsiClass objects, and the values are ArrayLists of PsiField objects
     *
     * For example, if a BlogPost class includes a Comment class, and the Comment class includes a User class, then the resulting tree will be:
     *
     * ```
     * parent: BlogPost Psi
     *    child: id
     *    child: Comment
     *        child: User
     *          child: name
     *```
     */
    fun dataStructure(clazz: PsiClass): SimpleClassStructure? {
        return simpleStructure(clazz)
    }

    private val psiStructureCache = mutableMapOf<PsiClass, SimpleClassStructure?>()

    /**
     * Creates a simple class structure for the given PsiClass and search scope.
     *
     * @param clazz the PsiClass for which the simple class structure needs to be created.
     * @return a SimpleClassStructure object representing the simple class structure of the given PsiClass.
     * The object contains the name of the class, the name of the fields, their types, and whether they are built-in or not.
     * If the field type is a primitive type or a boxed type, it is marked as built-in.
     * If the field type is a custom class, the method recursively creates a SimpleClassStructure object for that class.
     * If the field type cannot be resolved, it is skipped.
     */
    private fun simpleStructure(clazz: PsiClass): SimpleClassStructure? {
        // skip for generic
        if (clazz.name?.uppercase() == clazz.name && clazz.name?.length == 1) return null

        val qualifiedName = clazz.qualifiedName
        if ((qualifiedName != null) && psiStructureCache.containsKey(clazz)) {
            return psiStructureCache[clazz]!!
        }

        if (isJavaBuiltin(qualifiedName) == true || isPopularFramework(qualifiedName) == true) {
            return null
        }

        val fields = clazz.fields
        val children = fields.mapNotNull { field ->
            // if current field same to parent class, skip it
            if (field.type == clazz) return@mapNotNull null
            if (field.type is PsiTypeParameter) return@mapNotNull null

            val simpleClassStructure = when {
                // like: int, long, boolean, etc.
                field.type is PsiPrimitiveType -> {
                    SimpleClassStructure(field.name, field.type.presentableText, emptyList(), builtIn = true)
                }

                // like: String, List, etc.
                isPsiBoxedType(field.type) -> {
                    SimpleClassStructure(field.name, field.type.presentableText, emptyList(), builtIn = true)
                }

                field.type is PsiTypeParameter -> {
                    null
                }

                field.type is PsiClassType -> {
                    // skip for some frameworks like, org.springframework, etc.
                    val resolve = (field.type as PsiClassType).resolve() ?: return@mapNotNull null
                    if (resolve.qualifiedName == qualifiedName) return@mapNotNull null

                    if (isJavaBuiltin(resolve.qualifiedName) == true || isPopularFramework(resolve.qualifiedName) == true) {
                        return@mapNotNull null
                    }

                    val classStructure = simpleStructure(resolve) ?: return@mapNotNull null
                    classStructure.fieldName = field.name
                    classStructure.builtIn = false
                    classStructure
                }

                else -> {
                    logger.warn("Unknown supported type: ${field.type}")
                    return@mapNotNull null
                }
            }

            simpleClassStructure
        }

        val simpleClassStructure = SimpleClassStructure(clazz.name ?: "", clazz.name ?: "", children)
        psiStructureCache[clazz] = simpleClassStructure
        return simpleClassStructure
    }

    private val popularFrameworks = listOf(
        "org.springframework",
        "org.apache",
        "org.hibernate",
        "org.slf4j",
        "org.junit",
        "org.mockito"
    )

    fun isPopularFramework(qualifiedName: String?): Boolean {
        return popularFrameworks.any { qualifiedName?.startsWith(it) == true }
    }

    /**
     * Checks if the given PsiType is a boxed type.
     *
     * A boxed type refers to a type that is represented by a PsiClassReferenceType and its resolve() method returns null.
     * This typically occurs when the type is a generic type parameter or a type that cannot be resolved in the current context.
     *
     * @param type the PsiType to be checked
     * @return true if the given type is a boxed type, false otherwise
     */
    private fun isPsiBoxedType(type: PsiType): Boolean {
        if (type !is PsiClassReferenceType) return false

        val resolve = try {
            type.resolve() ?: return true
        } catch (e: Exception) {
            return false
        }

        return isJavaBuiltin(resolve.qualifiedName) == true
    }

    private fun isJavaBuiltin(qualifiedName: String?) = qualifiedName?.startsWith("java.")
}
