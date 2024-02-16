package cc.unitmesh.android.util

import com.android.sdklib.AndroidVersion

object AdSdkFinder {
    fun getSdkVersion(): Int? {
        val apiBaseExtensions = AndroidVersion.ApiBaseExtension.values()
        if (apiBaseExtensions.isEmpty()) return null

        var maxApiLevel = apiBaseExtensions[0].api

        for (i in 1 until apiBaseExtensions.size) {
            val currentApiLevel = apiBaseExtensions[i].api
            if (maxApiLevel < currentApiLevel) {
                maxApiLevel = currentApiLevel
            }
        }

        return maxApiLevel
    }
}