package cc.unitmesh.devti.language.provider.action

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolderBase

/**
 * The VariableActionEventDataHolder class serves as a temporary storage for data context related to VCS variable actions.
 * It uses a data class structure to encapsulate the data and provides a companion object to manage the data storage and retrieval.
 *
 * This class is designed to be used with a key-value pair mechanism where the data context is stored against a specific key.
 * The companion object provides static methods to put and get the data context.
 *
 * Usage:
 *
 * To store data:
 * ```Kotlin
 * val eventData = VariableActionEventDataHolder(dataContext)
 * VariableActionEventDataHolder.putData(eventData)
 * ```
 *
 * To retrieve data:
 * ```Kotlin
 * val eventData = VariableActionEventDataHolder.getData()
 * ```
 *
 * @param dataContext The DataContext object that holds the relevant information for VCS variable actions.
 *                    It is optional and can be null if no data needs to be stored initially.
 */
data class VariableActionEventDataHolder(val dataContext: DataContext? = null) {
    companion object {
        private val DATA_KEY: Key<VariableActionEventDataHolder> = Key.create(VariableActionEventDataHolder::class.java.name)
        private val dataHolder = UserDataHolderBase()

        fun putData(context: VariableActionEventDataHolder) {
            dataHolder.putUserData(DATA_KEY, context)
        }

        fun getData(): VariableActionEventDataHolder? {
            return dataHolder.getUserData(DATA_KEY)
        }
    }
}