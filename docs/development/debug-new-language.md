---
layout: default
title: Debug New Language
nav_order: 2
parent: Development
---

In JetBrains' IDE, some language support is not good enough, and some language support is not available at all.

- Good enough language will have IDE support, like golang with GoLand.
- Not good enough language will have no IDE support, like Rust with CLion (before RustRover)

So, we need to configure plugin for the language

## Debug Config

for Debug, We already run configs under `.idea/runConfigurations`, so we can just copy and modify them.

Here are some examples [RustRust.xml] :

```xml
<component name="ProjectRunConfigurationManager">
  <configuration default="false" name="Run Rust" type="GradleRunConfiguration" factoryName="Gradle">
    <ExternalSystemSettings>
      <option name="executionName" />
      <option name="externalProjectPath" value="$PROJECT_DIR$" />
      <option name="externalSystemIdString" value="GRADLE" />
      <option name="scriptParameters" value="-PbaseIDE=idea -Plang=rust" />
      <option name="taskDescriptions">
        <list />
      </option>
      <option name="taskNames">
        <list>
          <option value=":plugin:runIde" />
        </list>
      </option>
      <option name="vmOptions" value="" />
    </ExternalSystemSettings>
    <GradleScriptDebugEnabled>false</GradleScriptDebugEnabled>
    <method v="2" />
  </configuration>
</component>
```

We configure the `scriptParameters` to pass the `baseIDE` and `lang` to the gradle script.

```bash
./gradlew :plugin:runIde -PbaseIDE=idea -Plang=rust
```

## Configure in Gradle

We can configure the plugin in Gradle script, like build.gradle.kts :

```kotlin
project(":plugin") {
    apply {
        plugin("org.jetbrains.changelog")
    }

    version = prop("pluginVersion") + "-$platformVersion"

    intellij {
        pluginName.set(basePluginArchiveName)
        val pluginList: MutableList<String> = mutableListOf("Git4Idea")
        when (lang) {
            "idea" -> {
                pluginList += javaPlugins
            }
            "python" -> {
                pluginList += pycharmPlugins
            }
            "go" -> {
                pluginList += listOf("org.jetbrains.plugins.go")
            }
            "rust" -> {
                pluginList += rustPlugins
            }
        }

        plugins.set(pluginList)
    }
  
    // ...
}
```

In `rustPlugins`, we can see the plugin list for Rust:

```kotlin
val rustPlugins = listOf(
    prop("rustPlugin"),
    "org.toml.lang"
)
```

The `prop("rustPlugin")` is defined in `gradle.properties`, which will also load different version of plugin for different IDE version.

- gradle-222.properties
- gradle-233.properties

In `gradle-222.properties`, we can see the plugin version for Rust:

```properties
rustPlugin=org.rust.lang:0.4.185.5086-222
```

In `gradle-233.properties`, we can see the plugin version for Rust:

```properties
rustPlugin=com.jetbrains.rust:233.21799.284
```


## Debug Config for Rust

Tricks for Rust development.

Due to JetBrains' crafty move, there are two different versions of the Rust IDE plugin.

- **Under 233: Deprecated Rust**
  - check latest available version here https://plugins.jetbrains.com/plugin/8182--deprecated-rust
  - rustPlugin=org.rust.lang:0.4.185.5086-222
- **Above 233: Official Rust**
  - check latest available version here https://plugins.jetbrains.com/plugin/22407-rust/versions
  - rustPlugin=com.jetbrains.rust:233.21799.284
