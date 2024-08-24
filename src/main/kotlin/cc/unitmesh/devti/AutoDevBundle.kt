package cc.unitmesh.devti

import cc.unitmesh.devti.settings.AutoDevSettingsState
import cc.unitmesh.devti.settings.LanguageChangedCallback
import com.intellij.BundleBase
import com.intellij.DynamicBundle
import com.intellij.util.ArrayUtilRt
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey
import java.util.function.Supplier

@NonNls
private const val BUNDLE = "messages.AutoDevBundle"

object AutoDevBundle : DynamicBundle(BUNDLE) {

    private val myBundleClassLoader = AutoDevBundle::class.java.classLoader

    @Suppress("SpreadOperator")
    @JvmStatic
    fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any) = getMessage(key, *params)

    @Suppress("SpreadOperator", "unused")
    @JvmStatic
    fun messagePointer(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any) =
        getLazyMessage(key, *params)

    @Suppress("SpreadOperator")
    @JvmStatic
    fun messageWithLanguageFromLLMSetting(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any) =
        messageWithLanguage(key, LanguageChangedCallback.language, *params)

    @Suppress("SpreadOperator")
    @JvmStatic
    fun messageWithLanguage(@PropertyKey(resourceBundle = BUNDLE) key: String, language: String, vararg params: Any) =
        getMessage(key, language, *params)

    @Suppress("SpreadOperator", "unused")
    @JvmStatic
    fun messagePointerWithLanguage(
        @PropertyKey(resourceBundle = BUNDLE) key: String,
        language: String,
        vararg params: Any
    ) = getLazyMessage(key, language, *params)

    override fun getMessage(key: String, vararg params: Any?): String {
        return getMessage(key, AutoDevSettingsState.language, *params)
    }

    override fun getLazyMessage(key: String, vararg params: Any?): Supplier<String> {
        return getLazyMessage(key, AutoDevSettingsState.language, *params)
    }

    private fun getMessage(key: String, language: String, vararg params: Any?): String {
        val resourceBundle = getResourceBundle(myBundleClassLoader, buildPathToBundle(language))
        return BundleBase.messageOrDefault(resourceBundle, key, null, *params)
    }

    private fun getLazyMessage(key: String, language: String, vararg params: Any?): Supplier<String> = Supplier {
        getMessage(key, language, if (params.isEmpty()) ArrayUtilRt.EMPTY_OBJECT_ARRAY else params);
    }

    private fun buildPathToBundle(language: String): String {
        return BUNDLE + "_" + language;
    }
}
