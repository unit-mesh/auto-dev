package cc.unitmesh.devti.intentions.action.test

data class TestGenPromptContext(
    var lang: String = "",
    var imports: String = "",
    var frameworkContext: String = "",
    var currentClass: String = "",
    var relatedClasses: String = "",
    var sourceCode: String = "",
    var testClassName: String = "",
    var isNewFile: Boolean = true,
)