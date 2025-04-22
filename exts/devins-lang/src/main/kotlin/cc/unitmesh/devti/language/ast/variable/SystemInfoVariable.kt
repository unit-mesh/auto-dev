package cc.unitmesh.devti.language.ast.variable

import com.intellij.openapi.application.ApplicationInfo
import java.util.*

enum class SystemInfoVariable(
    override val variableName: String,
    override val description: String,
) : ToolchainVariable {
    OS_NAME("os.name", "The name of the operating system") {
        override var value: Any?
            get() = System.getProperty("os.name")
            set(_) {}
    },
    OS_VERSION("os.version", "The version of the operating system") {
        override var value: Any?
            get() = System.getProperty("os.version")
            set(_) {}
    },
    OS_ARCH("os.arch", "The architecture of the operating system") {
        override var value: Any?
            get() = System.getProperty("os.arch")
            set(_) {}
    },

    IDE_NAME("ide.name", "The name of the IDE") {
        override var value: Any?
            get() = System.getProperty("idea.platform.prefix", "idea")
            set(_) {}
    },
    IDE_VERSION("ide.version", "The version of the IDE") {
        override var value: Any?
            get() = ApplicationInfo.getInstance().build.asString()
            set(_) {}
    },
    IDE_CODE("ide.code", "The code of the IDE") {
        override var value: Any?
            get() = ApplicationInfo.getInstance().build.productCode
            set(_) {}
    },

    TIMEZONE("timezone", "The timezone") {
        override var value: Any?
            get() = TimeZone.getDefault().displayName
            set(_) {}
    },
    DATE("date", "The current date") {
        override var value: Any?
            get() = Calendar.getInstance().time
            set(_) {}
    },
    TODAY("today", "Today's date") {
        override var value: Any?
            get() = Calendar.getInstance().time
            set(_) {}
    },
    NOW("now", "The current time in milliseconds") {
        override var value: Any?
            get() = System.currentTimeMillis()
            set(_) {}
    },

    LOCALE("locale", "The default locale") {
        override var value: Any?
            get() = Locale.getDefault().toString()
            set(_) {}
    };

    companion object {
        fun from(variableName: String): SystemInfoVariable? {
            return values().firstOrNull { it.variableName == variableName }
        }

        fun all(): List<SystemInfoVariable> {
            return values().toList()
        }
    }
}