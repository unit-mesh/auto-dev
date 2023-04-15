package cc.unitmesh.devti.runconfig.command

import cc.unitmesh.devti.runconfig.DtCommandConfiguration
import com.intellij.execution.actions.LazyRunConfigurationProducer


abstract class BaseLazyRunConfigurationProducer<T> : LazyRunConfigurationProducer<DtCommandConfiguration>() {

}