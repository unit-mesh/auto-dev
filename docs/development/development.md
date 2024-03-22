---
layout: default
title: Development
nav_order: 10
has_children: true
permalink: /development
---

{: .no_toc }

# Development

1. `git clone https://github.com/unit-mesh/auto-dev/`
2. open in IntelliJ IDEA
3. `./gradlew runIde`

Key Concepts:

- Workflow flow design: [DevFlowProvider](src/main/kotlin/cc/unitmesh/devti/provider/DevFlowProvider.kt)
- Prompt Strategy design: [PromptStrategyAdvisor](src/main/kotlin/cc/unitmesh/devti/provider/PromptStrategy.kt)

### Release

1. change `pluginVersion` in [gradle.properties](gradle.properties)
2. git tag `version`
3. `./gradlew publishPlugin`
