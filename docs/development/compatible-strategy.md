---
layout: default
title: Compatible Strategy
nav_order: 3
parent: Development
---

In JetBrains' IDE, it is important to note that APIs can undergo changes, leading to potential incompatibility issues
between the plugin and the latest IDE versions. To address this, it is necessary to configure a compatibility strategy
for the plugin.

Within AutoDev, we employ the following strategies:

1. **Latest APIs:** We support versions 233 to 241 and beyond, representing JetBrains' IDEs where AI plugin development
   began.

2. **Stable APIs:** Our compatibility extends to versions 222 to 232, which are widely utilized within enterprise
   environments and ensure stability.

By adopting these strategies, we aim to enhance the plugin's compatibility with various JetBrains IDE versions, catering
to both cutting-edge and established enterprise development environments.

## Gradle Configuration for Compatibility

To ensure compatibility with different IDE versions, we can configure the plugin in the Gradle config, such as
`gradle.properties`:

```properties
# Supported platforms: 222, 233
platformVersion=233
```

By specifying the platform version, we can ensure that the plugin is compatible with the desired IDE versions.

### Platform Version Configuration

According to the different platform versions, we can configure the plugin by `gradle-{version}.properties`.
For example, in `gradle-222.properties`:

```properties
pluginSinceBuild=222.*
pluginUntilBuild=232.*
```

### Gradle SourceSets

We can also configure the plugin in the Gradle source sets, such as `src/main/{version}/kotlin` in `build.gradle.kts`:

```kotlin
allprojects {
    // ...
    sourceSets {
        main {
            java.srcDirs("src/gen")
            resources.srcDirs("src/$platformVersion/main/resources")
        }
        test {
            resources.srcDirs("src/$platformVersion/test/resources")
        }
    }
    kotlin {
        sourceSets {
            main {
                kotlin.srcDirs("src/$platformVersion/main/kotlin")
            }
            test {
                kotlin.srcDirs("src/$platformVersion/test/kotlin")
            }
        }
    }
    // ...
}
```

By configuring the plugin in the Gradle source sets, we can ensure that the plugin is compatible with different IDE
versions, catering to both cutting-edge and established enterprise development environments.
