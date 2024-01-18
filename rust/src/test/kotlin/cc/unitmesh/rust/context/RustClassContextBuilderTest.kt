package cc.unitmesh.rust.context;

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.extensions.ExtensionPoint
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.rust.lang.core.psi.RsStructItem

class RustClassContextBuilderTest: BasePlatformTestCase() {

    fun shouldFormatStruct() {
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

        ApplicationManager.getApplication().extensionArea.registerExtensionPoint(
            "cc.unitmesh.variableContextBuilder",
            "cc.unitmesh.devti.context.builder.VariableContextBuilder",
            ExtensionPoint.Kind.BEAN_CLASS,
            true
        )

        // then
        val result = RustClassContextBuilder().getClassContext(decl, false)!!
        assertEquals("Entry", result.name)
        assertEquals(
            result.format(), """
            'package: Entry
            class Entry {
              
              
            }
            """.trimIndent()
        )
    }
}

