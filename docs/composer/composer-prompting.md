---
layout: default
title: AutoDev Composer Prompting
parent: AutoDev Composer
nav_order: 1
---

# AutoDev Composer Prompting

What we use for AutoDev Composer's prompts:

- Project context, including libraries, frameworks, and languages.
- DSL: We use [DevIns DSL](/devins) to build our version's function tools.
- CoT: Function tools are better, but we believe DSLs are more powerful for both AI and humans to understand.

Based on user requirements, we analyze and execute them as a DSL, like:

```devin
/database:schema
/symbol:com.example.blog.Blog
```

Then AutoDev will call: `DatabaseInsCommand`, `SymbolInsCommand` to collect the data and return to user.

**Project Context**

Here is an example of the project context:

```markdown
- The USER's OS version is Mac OS X 15.2 x86_64
- The absolute path of the USER's workspaces is: /Users/phodal/IdeaProjects/untitled
- This workspace use Gradle Java JDK_11
- The user's shell is /bin/bash
- User's workspace context is: This project use MariaDB 11.5.2-MariaDB,You are working on a project that uses Spring
  Boot 2.7.10,Spring MVC,JDBC to build RESTful APIs.
- Current time is: 2025-01-21 16:20:42
```

When user connect Database, we will use the context to help user to connect the database. We also analysis the project
context to help user to generate the code.

**DSL**

Here is DevIns DSL example which we send to LLM to learn:

    <devin>
    /commit
    ```markdown
    follow Conventional Commits, like feat: add 'graphiteWidth' option
    ```
    
    </devin>

When use enable `AutoSketchMode`, the read-only commands will be auto-executed.

```kotlin
val READ_COMMANDS = setOf(
    BuiltinCommand.DIR,
    BuiltinCommand.LOCAL_SEARCH,
    BuiltinCommand.FILE,
    BuiltinCommand.REV,
    BuiltinCommand.STRUCTURE,
    BuiltinCommand.SYMBOL,
    BuiltinCommand.DATABASE
)
```

**CoT**

We make a example for CoT:
    
    <user.question>
    You are helping the USER create a python-based photo storage app. You
    have created a routes.py and main.js file, and updated the main.html file.
    </user.question>
    <you.anwser.step1>
    // In this step 1, you should analysis the context information is enough or not, if not, you should call DevIn tool
    // to get the context information.
    // For example:
    To help you create a Python-based photo storage application, I need to know more about your codebase. I will generate
    tool call code for you to get the necessary context information, please execute it to get the context information.
    <devin>
    /dir:src
    /localSearch:photo
    </devin>
    // If the context information is still not enough, you should ask the user to provide more information
    </you.anwser.step1>
    <user.anwser.step1>
    // waiting for user send the tool execute result
    </user.anwser.step1>
    <you.anwser.step2>
    // In this step 2, You should first explain to the user how to solve the problem, and then use the DevIn language
    // to call the tool to help the user solve the problem
    # Step 1. Create `routes.py`
    I have created `routes.py` to define the `/upload` and `/query` endpoints. Additionally, I have added `/` as the endpoint for `main.html`.
    
    # Step 2. Create `main.js`
    I have created a dedicated `main.js` file to store all the interactive frontend code. It defines the UI elements for the display window and buttons, and creates event listeners for these buttons.
    
    # Step 3. Update `index.html`
    I have moved all the JavaScript code to `main.js` and imported `main.js` in `index.html`. Separating JavaScript from HTML can improve code organization, readability, maintainability, and reusability.
    
    # Step 4. Run the application, test, and verify
    Now, I will start the Flask server and run our application. I will generate IntelliJ IDEA's `.http` code to help you with API testing; you can try uploading and searching for photos on the website to see if everything works fine.
    
    # Summary of changes
    By creating `routes.py` and `main.js`, I have made our photo application interactive. Users can now upload and search for photos using natural language queries. Additionally, I have made some modifications to the codebase to improve code organization and readability. Run the application and try uploading and searching for photos. If you encounter any errors or want to add new features, please let me know!
    
    // then you can start coding with DevIn language. When you need to or patch, write execute the code, you should use the DevIn language
    // If you need to create a new file, you should use `/write` command, then write the code in the code block
    <devin>
    /write:src/main/route.py // 当不存在文件是，才需要创建文件
    ```python
    // the route code
    // from flask import Flask
    ```
    </devin>

So, we can use DevIn to help user to generate the code.
