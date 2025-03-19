---
layout: default
title: Sketch Observer
parent: AutoDev Sketch/Composer
nav_order: 3
---

AutoDev Observer is an AI-powered diagnostic tool in AutoDev, designed to monitor and analyze issues that occur during
the software development process, specifically build failures and test failures. It helps developers identify root
causes, track resolution progress, and suggest possible fixes dynamically.

Currently, AutoDev Observer is integrated with the following tools:

- TestAgentObserver. When running tests, it monitors the test process, it will collect relative context.
- BuiltTaskAgentObserver. When build failure occurs, like Gradle build failure.
- AddDependencyAgentObserver. When add dependency failure occurs, it try to call AI to resolve the issue.

Todos:

- [ ] RemoteHookObserver