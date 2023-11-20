package cc.unitmesh.idea.context

import cc.unitmesh.devti.context.SimpleClassStructure
import com.intellij.psi.*
import com.intellij.psi.impl.source.PsiClassReferenceType
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.searches.MethodReferencesSearch
import com.intellij.psi.search.searches.ReferencesSearch

object JavaContextCollectionUtilsKt {
    fun findUsages(nameIdentifierOwner: PsiNameIdentifierOwner): List<PsiReference> {
        val project = nameIdentifierOwner.project
        val searchScope = GlobalSearchScope.allScope(project) as SearchScope

        return when (nameIdentifierOwner) {
            is PsiMethod -> {
                MethodReferencesSearch.search(nameIdentifierOwner, searchScope, true)
            }

            else -> {
                ReferencesSearch.search((nameIdentifierOwner as PsiElement), searchScope, true)
            }
        }.findAll().map { it as PsiReference }
    }

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
    fun dataStructure(clazz: PsiClass): SimpleClassStructure {
        return createSimpleStructure(clazz)
    }

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
    private fun createSimpleStructure(clazz: PsiClass): SimpleClassStructure {
        val fields = clazz.fields
        val children = fields.mapNotNull { field ->
            when {
                // like: int, long, boolean, etc.
                field.type is PsiPrimitiveType -> {
                    SimpleClassStructure(field.name, field.type.presentableText, emptyList(), builtIn = true)
                }

                // like: String, List, etc.
                isPsiBoxedType(field.type) -> {
                    SimpleClassStructure(field.name, field.type.presentableText, emptyList(), builtIn = true)
                }

                else -> {
                    val classStructure =
                        (field.type as PsiClassType).resolve()?.let { createSimpleStructure(it) }
                            ?: return@mapNotNull null
                    classStructure.builtIn = false
                    classStructure
                }
            }
        }

        return SimpleClassStructure(clazz.name ?: "", clazz.name ?: "", children)
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
        return type is PsiClassReferenceType && type.resolve() == null
    }
}
