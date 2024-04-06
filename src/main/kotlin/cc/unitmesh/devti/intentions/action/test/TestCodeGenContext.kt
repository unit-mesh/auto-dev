package cc.unitmesh.devti.intentions.action.test

import cc.unitmesh.devti.template.context.TemplateContext

data class TestCodeGenContext(
    var lang: String = "",
    var imports: String = "",
    var frameworkContext: String = "",
    var currentClass: String = "",
    var relatedClasses: String = "",
    var sourceCode: String = "",
    var testClassName: String = "",
    var isNewFile: Boolean = true,
) : TemplateContext