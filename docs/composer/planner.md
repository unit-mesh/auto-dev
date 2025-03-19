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

### Design inside Planner

In *[Is Design Dead?](https://www.martinfowler.com/articles/designDead.html)*, Fowler concludes that design is far from
dead, but its role has changed. Instead of being a rigid, upfront process, design in an agile world is **continuous,
incremental, and driven by refactoring and testing**. Agile methodologies, particularly Extreme Programming (XP),
emphasize **evolutionary design**, allowing architecture to adapt naturally as the system grows.

As we know, AI models function as **black boxes**, capable of generating vast amounts of code. While this ability is
powerful, it can also become **harmful at scale** if the generated code lacks proper design principles. Poorly
structured AI-generated code can lead to **technical debt, maintainability issues, and architectural inconsistencies**,
making it difficult for teams to evolve their software effectively.

#### The Need for a Well-Designed AI Agent

To mitigate these risks, we need to design an **AI agent** that enforces and supports structured software development.
Such an agent should focus on:

- **Planned and Evolutionary Design** – Combining strategic planning with the flexibility to evolve the design over
  time. Instead of enforcing rigid upfront designs, the AI agent should guide the developer in **iterative refinement**.
- **Growing an Architecture** – Ensuring that as the system scales, its architecture remains **cohesive and adaptable**,
  avoiding monolithic, tangled structures.
- **Simple Design** – Encouraging minimalism and clarity in the codebase, following principles like **YAGNI (You Ain’t
  Gonna Need It)** and **KISS (Keep It Simple, Stupid)**.

For example, when asking an AI to design a **complex frontend page**, one good practice is **ViewModel splitting**.
Instead of generating a monolithic UI structure, the AI should **separate concerns properly**, ensuring maintainability
and reusability. By guiding AI-generated code with solid **architectural principles**, we can create scalable and
sustainable software solutions.

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
