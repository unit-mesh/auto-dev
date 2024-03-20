package cc.unitmesh.devti.language

import com.intellij.DynamicBundle
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey


@NonNls
private const val DevInBUNDLE: String = "messages.DevInBundle"

object DevInBundle : DynamicBundle(DevInBUNDLE) {
    @Suppress("SpreadOperator")
    @JvmStatic
    fun message(@PropertyKey(resourceBundle = DevInBUNDLE) key: String, vararg params: Any) = getMessage(key, *params)

    @Suppress("SpreadOperator", "unused")
    @JvmStatic
    fun messagePointer(@PropertyKey(resourceBundle = DevInBUNDLE) key: String, vararg params: Any) =
        getLazyMessage(key, *params)
}
