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
        val structure = JavaContextCollectionUtilsKt.dataStructure(blogpost)
        TestCase.assertEquals(structure.children.size, 2)
        TestCase.assertEquals(structure.toString(), "class BlogPost {\n" +
                "  id: long\n" +
                "  Comment: Comment\n" +
                "}\n" +
                "\n" +
                "class Comment {\n" +
                "  id: long\n" +
                "  User: User\n" +
                "}\n" +
                "\n" +
                "class User {\n" +
                "  id: long\n" +
                "  name: String\n" +
                "}\n")
    }
}
