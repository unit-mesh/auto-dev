---
layout: default
title: Development FAQ
nav_order: 999
parent: Development
---

## EDT and ReadAction issue 

> Synchronous execution under ReadAction: /usr/local/bin/git -c credential.helper= -c core

A solution will be like:

```kotlin
/**
 * Refs to [com.intellij.execution.process.OSProcessHandler.checkEdtAndReadAction], we should handle in this
 * way, another example can see in [git4idea.GitPushUtil.findOrPushRemoteBranch]
 */
val future = CompletableFuture<List<GitCommit>>()
val task = object : Task.Backgroundable(project, "xx", false) {
    override fun run(indicator: ProgressIndicator) {
        // some long time operation
        future.complete(/* commits */)
    }
}

ProgressManager.getInstance()
    .runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))

runBlockingCancellable {
    val commits = future.await()
    // do something
}
```

## API 兼容方案


https://github.com/JetBrains/aws-toolkit-jetbrains/tree/ccee3307fe58ad48f93cd780d4378c336ee20548/jetbrains-core

```kotlin
// Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0
package software.aws.toolkits.jetbrains.core.docker.compatability

typealias DockerFileAddOrCopyCommand = com.intellij.docker.dockerFile.parser.psi.DockerFileAddOrCopyCommand
typealias DockerFileCmdCommand = com.intellij.docker.dockerFile.parser.psi.DockerFileCmdCommand
typealias DockerFileExposeCommand = com.intellij.docker.dockerFile.parser.psi.DockerFileExposeCommand
typealias DockerFileFromCommand = com.intellij.docker.dockerFile.parser.psi.DockerFileFromCommand
typealias DockerFileWorkdirCommand = com.intellij.docker.dockerFile.parser.psi.DockerFileWorkdirCommand
```

## java.lang.Throwable: Must be executed under progress indicator: com.intellij.openapi.progress.EmptyProgressIndicator@6c3fd0d8 but the process is running under null indicator instead. Please see e.g. ProgressManager.runProcess()

```kotlin
 val future = CompletableFuture<String>()
val task = object : Task.Backgroundable(project, "Loading", false) {
    override fun run(indicator: ProgressIndicator) {
        // collectApis point to your long time operation
        future.complete(this.collectApis(project, endpointsProviderList))
    }
}

ProgressManager.getInstance()
    .runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))

return future.get()
```