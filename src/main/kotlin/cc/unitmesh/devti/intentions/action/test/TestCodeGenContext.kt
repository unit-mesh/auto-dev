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
    /**
     * in issue [195](https://github.com/unit-mesh/auto-dev/issues/195), we introduction autodev ext context for
     * user to introduce their own context, this is the context for autodev ext
     *
     * see in [cc.unitmesh.devti.custom.CustomExtContext]
     *
     * ```json
     * {
     *     "name": "@autodev.ext-context.test"
     *     "description": "AutoTest",
     *     "url": "http://127.0.0.1:8765/api/agent/auto-test",
     *     "responseAction": "Direct"
     * }
     * ```
     */
    var extContext: String = "",
) : TemplateContext