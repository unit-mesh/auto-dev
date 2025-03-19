---
layout: default
title: AutoDev Bridge
nav_order: 2
has_children: true
parent: Agent
permalink: /bridge
---

Video demo (Bilibili) - 中文：[Watch the video](https://www.bilibili.com/video/BV1RwRNYEE1A/)

# AutoDev Bridge - Legacy Code Migration

Required tool:

- [SCC](https://github.com/boyter/scc)

Required plugin:

- [OpenRewrite](https://plugins.jetbrains.com/plugin/23814-openrewrite) (Intellij IDEA Ultimate)
- [Endpoints](https://plugins.jetbrains.com/plugin/16890-endpoints) (Intellij IDEA Ultimate)

### Custom Bridge

follow [Prompt Override](/customize/prompt-override), the AI Composer can be customized. in the `prompt/code` folder,
you can create a file named `bridge.vm` to override the composer prompt.

### Custom Reasoner model

Refs to [New Config (2.0.0-beta.4+)](/quick-start#new-config-200-beta4)

### Docker 

#### Colima 

[Cannot connect to the Docker daemon at unix:///var/run/docker.sock. Is the docker daemon running?](https://github.com/abiosoft/colima/blob/main/docs/FAQ.md#cannot-connect-to-the-docker-daemon-at-unixvarrundockersock-is-the-docker-daemon-running)

```bash
export COLIMA_HOME=$HOME/.colima
export DOCKER_HOST="unix://${COLIMA_HOME}/default/docker.sock"
sudo ln -sf $COLIMA_HOME/default/docker.sock /var/run/docker.sock
```

##### FAQ

```
Deploying '<unknown> Dockerfile: ../../../../shire.Dockerfile'…
ERROR: BuildKit is enabled but the buildx component is missing or broken.
Install the buildx component to build images with BuildKit:
https://docs.docker.com/go/buildx/
Failed to deploy '<unknown> Dockerfile: ../../../../shire.Dockerfile': Image build failed with exit code 1.
```

Refs: https://github.com/abiosoft/colima/discussions/273

```bash
brew install docker-buildx
docker buildx install
```




