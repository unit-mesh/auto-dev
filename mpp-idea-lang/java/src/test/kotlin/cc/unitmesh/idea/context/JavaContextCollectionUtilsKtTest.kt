package cc.unitmesh.idea.context;

import com.intellij.lang.java.JavaLanguage
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import junit.framework.TestCase

class SimpleClassStructureTest : LightJavaCodeInsightFixtureTestCase() {
    private val fileFactory: PsiFileFactory get() = PsiFileFactory.getInstance(project)

    fun testBuildDataStructure() {
        val javaCode = """class BlogPost {
    long id;
    Comment comment;
}
class Comment {
    long id;
    User user;
}
class User {
    long id;
    String name;
}
"""

        val psiFile = fileFactory.createFileFromText(JavaLanguage.INSTANCE, javaCode)
        val classes = PsiTreeUtil.findChildrenOfType(psiFile, PsiClass::class.java)

        val blogpost = classes.first { it.name == "BlogPost" }
        val structure = JavaContextCollection.dataStructure(blogpost)!!
        TestCase.assertEquals(structure.children.size, 2)
        TestCase.assertEquals(structure.toString(), """
            class BlogPost {
              id: long
              comment: Comment
            }
            
            class Comment {
              id: long
              user: User
            }
            
            class User {
              id: long
              name: String
            }
            
            """.trimIndent()
        )
    }
}
