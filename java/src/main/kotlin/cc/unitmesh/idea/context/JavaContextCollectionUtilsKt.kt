package cc.unitmesh.idea.context

import cc.unitmesh.devti.context.SimpleClassStructure
import com.intellij.psi.*
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
     * @return a HashMap where the keys are the PsiClass objects and the values are ArrayLists of PsiField objects
     *
     * For example, if a BlogPost class includes a Comment class, and the Comment class includes a User class, then the resulting tree will be:
     *
     * parent: BlogPost Psi
     *    child: I'd
     *    child: Comment psi
     *        child: User psi
     */
    fun dataStructure(clazz: PsiClass): HashMap<String, SimpleClassStructure> {
        val classTree = HashMap<String, SimpleClassStructure>()
        buildSimpleClassStructure(clazz, classTree)
        return classTree
    }

    private fun buildSimpleClassStructure(clazz: PsiClass, classTree: HashMap<String, SimpleClassStructure>) {
        val fields = clazz.fields
        val children = fields.mapNotNull {
            if (it.type is PsiClass) {
                val childSimpleClassStructure = SimpleClassStructure(it.name, it.type.canonicalText, emptyList())
                buildSimpleClassStructure(it.type as PsiClass, classTree)
                childSimpleClassStructure
            } else {
                SimpleClassStructure(it.name, it.type.canonicalText, emptyList())
            }
        }

        val classSimpleClassStructure =
            SimpleClassStructure(clazz.name ?: "Unknown", clazz.qualifiedName ?: "Unknown", children)
        classTree[clazz.name ?: "Unknown"] = classSimpleClassStructure
    }
}
