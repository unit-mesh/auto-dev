---
layout: default
title: Sketch Planner
parent: AutoDev Sketch/Composer
nav_order: 2
---

AutoDev Planner is a task planning system that enhances AI-assisted coding by making progress visible and interactive.
Inspired by AI coding tools like GitHub Copilot Workspace, Cursor, and JetBrains Junie, it aims to improve transparency
and adaptability in AI-driven development workflows.

**Key Features**:

1. **Visible Task Planning** – Users can track task progress through the Planner ToolWindow and pin important tasks.
2. **Dynamic Task Adjustment** – AI dynamically refines plans based on context, though responsiveness depends on the
   model used.
3. **Manual Execution** – Users can manually execute incomplete tasks to fine-tune the development process.
4. **Task Review** – Manual review of task plans using AI, allowing users to optimize their workflow while controlling
   token usage.

**Core Technical Aspects**:

- **Inference Model-Based Planning** – Tasks are structured using reasoning models like DeepSeek R1, which requires
  custom prompts for better execution.
- **Interactive Task Management** – Users can mark tasks as complete, open related files, edit plans, and review
  AI-generated strategies.

By making AI-driven coding tasks more transparent and controllable, AutoDev Planner enhances the development experience,
allowing users to engage with and refine AI-generated code plans.


### Plan Example

Example of a Generated Plan:

1. Identify Core Workflow Classes
    - [✓] Search for class definitions containing "Workflow" and "Sketch"
    - [✓] Analyze the `execute` method in `SketchRunner`

2. Analyze AI Flow Execution Stages
    - [✓] Identify the context collection phase
    - [✓] Analyze the tool invocation decision module
    - [✓] Trace the code generation pipeline

3. Validate Workflow Integrity
    - [✓] Check the exception handling mechanism
    - [✓] Confirm version control integration points  

![](https://unitmesh.cc/auto-dev/autodev-plan-sketch.png)
