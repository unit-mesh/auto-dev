---
layout: default
title: Sketch Workspace
parent: AutoDev Sketch/Composer
nav_order: 3
permalink: /workspace
---

# Sketch Workspace

## Workspace Dictionary 

The workspace dictionary is a collection of domain-specific terms and phrases that are used to enhance the AI model's
understanding of the specific context in which it is being used. 

![](https://unitmesh.cc/auto-dev/workspace-enhance.png)

We use `prompts/domain.csv` to define the workspace dictionary.

### Generate Workspace Dictionary

We create AnAction call `DomainDictGenerateAction` to generate the workspace dictionary. Which set in the `ProjectViewToolbar`
toolbar.

After you click the button, it will generate a `prompts/domain.csv` file in the project root directory. 

### Use in AutoDev Input

When you use the AutoDev input, you can click `Enhance` button to use the workspace dictionary. Which will auto add the
domain word to the input.

## Workspace Files

The workspace files are the files that are used to send to the AI model in AutoDev input section. When you use the AutoDev input,
you can click `Files` button to select the files to send to the AI model.

![](https://unitmesh.cc/auto-dev/workspace-files.png)

## Workspace Rule

aka [Project Rule](/composer/project-rule)

![](https://unitmesh.cc/auto-dev/workspace-rule.png)

## Workspace for Planner

The workspace for manage the AI changed files, add files, remove files, and so on.

![](https://unitmesh.cc/auto-dev/workspace-changes.png)
