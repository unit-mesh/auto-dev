package cc.unitmesh.devti.connector.custom

import cc.unitmesh.devti.connector.CodeCopilot
import cc.unitmesh.devti.settings.DevtiSettingsState

class CustomConnector : CodeCopilot {
    val url = DevtiSettingsState.getInstance()?.customEngineServer ?: ""
    val key = DevtiSettingsState.getInstance()?.customEngineToken ?: ""

    private fun prompt(instruction: String, input: String): String {
//        val retrofit = Retrofit.Builder()
//            .baseUrl(url)
//            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
//            .build()

        return ""
    }

    override fun codeCompleteFor(text: String, className: String): String {
        return ""
    }

    override fun autoComment(text: String): String {
        return ""
    }

    override fun codeReviewFor(text: String): String {
        return ""
    }

    override fun findBug(text: String): String {
        return ""
    }

}