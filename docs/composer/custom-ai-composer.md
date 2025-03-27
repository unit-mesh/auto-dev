---
layout: default
title: Sketch Quick start
parent: AutoDev Sketch/Composer
nav_order: 1
---

# Tips

## Custom AI Composer

follow [Prompt Override](/customize/prompt-override), the AI Composer can be customized. in the `prompt/code` folder,
you can create a file named [`sketch.vm`](https://github.com/unit-mesh/auto-dev/blob/master/core/src/main/resources/genius/zh/code/sketch.vm) 
to override the composer prompt.

## Use RipgrepSearch

Since we don't have a full-text search feature, you can use `RipgrepSearch` to search for files in the project. You can
install it via Homebrew:

```bash
brew install ripgrep
```

Then you can use it in the AutoDev Composer.




