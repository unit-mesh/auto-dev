package cc.unitmesh.devti.runconfig.command

import cc.unitmesh.devti.AutoDevBundle

abstract class BaseConfig {
    open val configurationName: String = AutoDevBundle.message("name")
}
