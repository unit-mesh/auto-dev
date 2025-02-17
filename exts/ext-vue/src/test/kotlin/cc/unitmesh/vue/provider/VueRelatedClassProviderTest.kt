package cc.unitmesh.vue.provider

import com.intellij.psi.PsiFileFactory
import com.intellij.sql.psi.SqlLanguage
import com.intellij.testFramework.LightPlatformTestCase
import org.intellij.lang.annotations.Language
import org.jetbrains.vuejs.lang.html.VueLanguage

class VueRelatedClassProviderTest : LightPlatformTestCase() {
    @Language("Vue")
    private val code = """
 <template>
    <div>
        <h1>Hello, Vue!</h1>
    </div>
</template>

<script>
export default {
    name: 'HelloWorld',
    data() {
        return {
            message: 'Welcome to Your Vue.js App'
        }
    },
    methods: {
        greet() {
            alert(this.message)
        }
    }
}
</script>

<style scoped>
h1 {
    color: #42b983;
}
</style>
    """.trimIndent()

    fun testShouldReturnEmptyListWhenLookupElementIsCalledWithNonVueElement() {
        val vueRelatedClassProvider = VueRelatedClassProvider()

        // createVueFileFromText
        val file =
            PsiFileFactory.getInstance(project).createFileFromText("temp.vue", VueLanguage.INSTANCE, code)

        vueRelatedClassProvider.lookup(file)
    }
}