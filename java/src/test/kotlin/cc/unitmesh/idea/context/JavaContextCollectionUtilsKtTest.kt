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
    User user;
}
class User {
    String name;
}
"""

        val psiFile = fileFactory.createFileFromText(JavaLanguage.INSTANCE, javaCode)
        val classes = PsiTreeUtil.findChildrenOfType(psiFile, PsiClass::class.java)

        val blogpost = classes.filter { it.name == "BlogPost" }.first()
        val tree = JavaContextCollectionUtilsKt.dataStructure(blogpost)
        TestCase.assertEquals(tree["BlogPost"]?.toString(), "class BlogPost {\n" +
                "  id: long\n" +
                "  comment: Comment\n" +
                "}\n")
    }
}
