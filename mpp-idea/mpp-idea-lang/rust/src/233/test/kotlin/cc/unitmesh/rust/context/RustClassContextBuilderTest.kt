package cc.unitmesh.rust.context

import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.rust.lang.core.psi.RsEnumItem
import org.rust.lang.core.psi.RsStructItem

class RustClassContextBuilderTest: BasePlatformTestCase() {

    fun testShouldFormatStruct() {
        // given
        val code = myFixture.configureByText(
            "test.rs", """
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
        """.trimIndent()
        )

        // when
        val decl = PsiTreeUtil.getChildrenOfTypeAsList(code, RsStructItem::class.java).first()

        // then
        val result = RustClassContextBuilder().getClassContext(decl, false)!!
        assertEquals("Entry", result.name)
        assertEquals(
            result.format(), """
            'package: Entry
            class Entry {
              id: String
              embedding: Embedding
              embedded: Document
              
            }
            """.trimIndent()
        )
    }

    fun testShouldHandleForEnum() {
        val code = myFixture.configureByText(
            "test.rs", """
            
            enum List {
                Cons(u32, Box<List>),
                Nil,
            }
            
            impl List {
                fn new() -> List {
                    Nil
                }
            
                fn prepend(self, elem: u32) -> List {
                    Cons(elem, Box::new(self))
                }
            }
            """.trimIndent()
        )

        val decl = PsiTreeUtil.getChildrenOfTypeAsList(code, RsEnumItem::class.java).first()
        val result = RustClassContextBuilder().getClassContext(decl, false)!!

        assertEquals("List", result.name)
        assertEquals(
            result.format(), """
            'package: List
            class List {
              Cons(u32, Box<List>)
              Nil
              
            }
            """.trimIndent()
        )
    }
}

