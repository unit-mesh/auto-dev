---
layout: default
title: AutoDev Composer：the Intellij IDEA Cursor Alternative
nav_order: 9
parent: What's New
---

A little over two weeks ago, after the release of DeepSeek V3, we introduced multi-file editing capabilities for Shire.

Following extensive testing, we discovered that DeepSeek V3 performs exceptionally well in programming scenarios,
especially in multi-file editing contexts.

This inspired us to add a new feature—**AutoDev Composer**—to AutoDev, which had long lacked major updates. In
developing this feature, we drew inspiration from a number of mature tools:

- The impressive **Sketch rendering mechanism** on Shire
- Complex **system prompts** from tools like Cursor and WindSurf
- The bug-ridden **StreamDiff mode** from Continue
- …and more

Now, there’s no need to switch to a VSCode-like IDE to craft prompts and then return to IntelliJ IDEA for debugging.
With AutoDev Composer, you can handle everything directly within IntelliJ IDEA.

As an amateur project, we’ve put in a lot of effort to make this happen! 😊

---

### **Decoding AutoDev Composer System Prompts**

#### **Project Context**

In Composer mode, you can still experience our understanding of software engineering and our extensive expertise in
software component analysis. Below is the basic contextual prompt for AutoDev Composer:

```markdown
- The USER's OS version is Mac OS X 15.2 x86_64
- The absolute path of the USER's workspaces is: /Users/phodal/IdeaProjects/untitled
- This workspace uses Gradle and Java JDK_11
- The user's shell is /bin/bash
- User's workspace context is: This project uses MariaDB 11.5.2-MariaDB, Spring Boot 2.7.10, Spring MVC, and JDBC to
  build RESTful APIs.
- Current time is: 2025-01-20 11:23:59  
```  

We gather system, workspace, toolchain, database, and language environment information to help you work more
efficiently. For example, when writing CRUD code, AutoDev generates corresponding code based on your database
information.

---

#### **Tool Context**

Unlike FunctionTool, we firmly believe that DSLs (Domain-Specific Languages) are the best approach for generative AI
solutions. Therefore, AutoDev adopts a DevIns DSL tool-calling mechanism:

```markdown
<tool>name: file, desc: Read the content of a file by project relative path, example:  
<devin>  
Locate a specific file (the file must exist in the specified path)  
/file:.github/dependabot.yml#L1C1-L2C12  
Search globally by file name (case-sensitive, no path required)  
/file:PythonFrameworkContextProvider.kt  
</devin>  
```  

Since the tool’s documentation is part of the code and test suite, it allows for the generation of precise and reliable
prompts.

---

### **Thought Process**

Given the complexity of the AutoDev DSL tools, we referenced WindSurf’s prompt generation approach and introduced a
step-by-step **thought process**:

```markdown
# Step 1. Create `routes.py`

I’ve created `routes.py` to define the `/upload` and `/query` endpoints. Additionally, I’ve added `/` as the endpoint
for `main.html`.

# Step 2. Create `main.js`

I’ve created a dedicated `main.js` file to store all the interactive front-end code. It defines UI elements for
displaying windows and buttons and creates event listeners for these buttons.  
```  

This allows us to provide additional examples for the AI model, which has proven to learn remarkably well.

---

### **AutoDev Developer Experience: Sketch Mode**

Although AutoDev is now an amateur project, we firmly believe that understanding developer experience is at the core of
AI-assisted development. For this reason, we’ve introduced Sketch mode from Shire’s intelligent agent language into
AutoDev Composer. With Sketch, you can transform code into *everything*.

Simply put, Markdown is rendered with various UIs to enable better interactivity.

#### **Diff Sketch Mode**

With Diff Sketch mode, you can quickly understand AI-generated code changes and decide how to handle them: view the
source file, examine the diff, or apply the changes directly.

Since we currently lack the capacity to offer a diff model like Cursor, we’ve implemented a repair model. When patches
cannot be accurately identified, the model is called again for corrections.

#### **Terminal Sketch Mode**

In AutoDev, the Terminal Sketch mode allows you to execute scripts or pop out a terminal (similar to Cursor), enabling
you to run commands and view the results conveniently.

#### **Mermaid and PlantUML Sketch Modes**

When you install the Mermaid or PlantUML plugins, you can use their respective Sketch modes to convert your code into
flowcharts, sequence diagrams, and more.

---

### **Other**

**Download and try it out:**  
[https://github.com/unit-mesh/auto-dev/releases](https://github.com/unit-mesh/auto-dev/releases)

---  