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