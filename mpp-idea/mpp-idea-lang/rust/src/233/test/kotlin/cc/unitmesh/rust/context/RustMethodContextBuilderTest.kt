package cc.unitmesh.rust.context

import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.rust.lang.core.psi.RsFunction
import org.rust.lang.core.psi.RsImplItem
import org.rust.lang.core.psi.RsMembers

class RustMethodContextBuilderTest : BasePlatformTestCase() {

    fun testShouldFormatMethodStruct() {
        // given
        val psiFile = myFixture.configureByText(
            "test.rs", """
        use crate::embedding::Embedding;
        use crate::Document;
        
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
        val decl = PsiTreeUtil.getChildrenOfTypeAsList(psiFile, RsImplItem::class.java).first()
        val member = PsiTreeUtil.getChildrenOfTypeAsList(decl, RsMembers::class.java).first()
        val firstMethod = PsiTreeUtil.getChildrenOfTypeAsList(member, RsFunction::class.java).first()

        // then
        val result = RustMethodContextBuilder().getMethodContext(firstMethod, false, false)!!
        assertEquals("new", result.name)
        assertEquals(
            result.format().trimEnd(), """
            path: /src/test.rs
            language: Rust
            fun name: new
            fun signature: fn <b>new</b>(id: String, embedding: Embedding, embedded: Document) -&gt; Self
            """.trimIndent()
        )
    }
}
