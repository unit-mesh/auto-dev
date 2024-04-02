package cc.unitmesh.devti

import cc.unitmesh.devti.settings.AutoDevSettingsState
import com.intellij.DynamicBundle
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey
import java.lang.invoke.MethodHandles
import java.util.*

@NonNls
private const val BUNDLE = "messages.AutoDevBundle"

object AutoDevBundle : DynamicBundle(BUNDLE) {
    @Suppress("SpreadOperator")
    @JvmStatic
    fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any) = getMessage(key, *params)

    @Suppress("SpreadOperator", "unused")
    @JvmStatic
    fun messagePointer(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any) =
        getLazyMessage(key, *params)

    override fun findBundle(
        @NonNls pathToBundle: String,
        loader: ClassLoader,
        control: ResourceBundle.Control
    ): ResourceBundle {
        val base = super.findBundle(pathToBundle, loader, control)
        // load your bundle from baseName_<language>.properties, e.g. "baseName_zh.properties"
        val localizedPath = pathToBundle + "_" + AutoDevSettingsState.language
        val localeBundle = super.findBundle(
            localizedPath,
            AutoDevBundle::class.java.getClassLoader(), control
        )
        if (base != localeBundle) {
            setParent(localeBundle, base)
            return localeBundle
        }
        return base
    }
    private fun setParent(localeBundle: ResourceBundle, base: ResourceBundle) {
        try {
            val method = ResourceBundle::class.java.getDeclaredMethod("setParent", ResourceBundle::class.java)
            method.isAccessible = true
            MethodHandles.lookup().unreflect(method).bindTo(localeBundle).invoke(base)
        } catch (e: Throwable) {
            // ignored, better handle this in production code
        }
    }

}
