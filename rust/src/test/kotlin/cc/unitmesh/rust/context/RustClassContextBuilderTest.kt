package cc.unitmesh.rust.context;

import cc.unitmesh.devti.context.ClassContext
import cc.unitmesh.devti.context.builder.ClassContextBuilder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.cidr.lang.psi.OCDeclaration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.rust.lang.core.psi.RsEnumItem
import org.rust.lang.core.psi.RsStructItem
import org.rust.lang.core.psi.ext.RsStructOrEnumItemElement
import org.rust.lang.core.psi.ext.fields

class RustClassContextBuilderTest: BasePlatformTestCase() {

    fun testShouldFormatStruct() {
        // given
        val code = myFixture.configureByText("test.rs", """
        use crate::embedding::Embedding;
        use crate::similarity::{CosineSimilarity, RelevanceScore};
        
        #[derive(Debug, Clone)]
        pub struct Entry {
            id: String,
            embedding: Embedding,
            embedded: Document,
        }
        
        impl Entry {
            fn new(id: String, embedding: Embedding, embedded: Document) -> Self {
                Entry { id, embedding, embedded }
            }
        }
        """.trimIndent())

        // when
        val decl = PsiTreeUtil.getChildrenOfTypeAsList(code, RsStructItem::class.java).first()

        // then
        val result = RustClassContextBuilder().getClassContext(decl, false)!!
        assertEquals("Entry", result.name)
        assertEquals(result.format(), """
            'package: Entry
            class Entry {
              
              
            }
            """.trimIndent())
    }
}
