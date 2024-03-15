package cc.unitmesh.devti.runconfig

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.AutoDevIcons
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.ConfigurationTypeBase
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import javax.swing.Icon

class AutoDevConfigurationType :  ConfigurationTypeBase(
    AutoCRUDConfigurationFactory.ID,
    AutoDevBundle.message("autodev.crud"),
    "AutoCRUD generator",
    AutoDevIcons.AI_COPILOT
)