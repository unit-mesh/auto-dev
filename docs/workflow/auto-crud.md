---
layout: default
title: AutoCRUD
nav_order: 1
parent: Workflow
---

Required plugin:

Java
{: .label .label-yellow }

Demo Video: [https://www.bilibili.com/video/BV1Ye411h7Qu/](https://www.bilibili.com/video/BV1Ye411h7Qu/)

Follow: [AutoDev for CRUD (Java) ](/features/auto-dev)

Main function steps:

```kotlin
abstract fun getOrCreateStoryDetail(id: String): String

abstract fun updateOrCreateDtoAndEntity(storyDetail: String)

abstract fun fetchSuggestEndpoint(storyDetail: String): TargetEndpoint

abstract fun updateOrCreateEndpointCode(target: TargetEndpoint, storyDetail: String)

abstract fun updateOrCreateServiceAndRepository()
```