---
layout: default
title: DevOps
parent: Basic Features
nav_order: 10
permalink: /features/devops
---

1. click New the menu (or right-click in left project nam) -> SRE Genius(DevOps)

<img src="https://unitmesh.cc/auto-dev/ci-cd.png" alt="AutoDevChat" width="600px"/>

## Generate GitHub Actions

Which will generate build system info and template file, then render to prompt

impl code: [GenerateGitHubActionsAction](https://github.com/unit-mesh/auto-dev/blob/master/src/main/kotlin/cc/unitmesh/genius/actions/GenerateGitHubActionsAction.kt)

```kotlin
val githubActions = BuildSystemProvider.guess(project);
val templateRender = TemplateRender("genius/cicd")
templateRender.context = DevOpsContext.from(githubActions)
val template = templateRender.getTemplate("generate-github-action.vm")
```

## Generate Dockerfile

Which will generate build system info and template file, then render to prompt

impl code: [GenerateDockerfileAction](https://github.com/unit-mesh/auto-dev/blob/master/src/main/kotlin/cc/unitmesh/genius/actions/GenerateDockerfileAction.kt)

```kotlin
val dockerContexts = BuildSystemProvider.guess(project)
val templateRender = TemplateRender("genius/sre")
templateRender.context = DevOpsContext.from(dockerContexts)
val template = templateRender.getTemplate("generate-dockerfile.vm")
```
