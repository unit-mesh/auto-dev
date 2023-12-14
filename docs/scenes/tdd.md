---
layout: default
title: TDD
nav_order: 3
parent: Scenes
permalink: /scenes/tdd
---

- Scene: TDD
- Used features: Custom Team Prompts

1. Tasking
2. TDD red: write failed test
3. TDD green: make test pass
4. refactor

## Tasking

    ---
    interaction: AppendCursorStream
    ---
    **System:**
    
    You are a software developer. Reply only with code
    
    **User:**
    
    The code given below is written in Java, convert it into Kotlin without changing its functionality. Output the converted snippet with ``` at the start and end:

    ```user```
    
    你是一个资深的软件开发工程师，你擅长使用 TDD 的方式来开发软件，你现在需要帮助帮手开发人员做好 Tasking，以方便于编写测试用例。
    
    - Tasking 产生的任务都是具有独立业务价值的，每完成一条，都可以独立交付、产生价值。
      - 采用面向业务需求的 Tasking 采用业务语言描述任务列表，更有助于开发人员和业务人员对需求进行详细的沟通和确认。
      - 采用 Given When Then 的书写格式，其中 When 中所代表系统行为。
      - 要考虑业务场景覆盖率，可能合并重复的测试场景。
    
    请严格按照以下的格式输出。
    
    示例如下：
    
    Q: 开发一个出租车计费功能，它的计算规则是这样的：不超过8公里时每公里收费0.8元，超过8公里则每公里加收50%长途费，停车等待时每分钟加收0.25元。
    A: ###
    ${commentSymbol} Given 出租车行驶了5公里（8公里以内），未发生等待，When 计费，Then 收费4元
    ${commentSymbol} Given 出租车行驶了5公里（8公里以内），等待10分钟，When 计费，Then 收费6.5元
    ${commentSymbol} Given 出租车恰好行驶了8公里，未发生等待，When 计费，Then 收费6.4元
    ${commentSymbol} Given 出租车恰好行驶了8公里，等待10分钟，When 计费，Then 收费8.9元
    ###
    Q: ${selection}
    A: ###
    
## TDD Red
    
    ---
    interaction: AppendCursorStream
    ---
    ```user```
    
    你是一个资深的软件开发工程师，你擅长使用 TDD 的方式来开发软件，你需要根据用户的需求，帮助用户编写测试代码。
    
    ${frameworkContext}
    
    当前类相关的代码如下：
    
    ${beforeCursor}
    
    用户的需求是：${selection}
    
    请使用 @Test 开头编写你的代码块：

## TDD Green
    
    ---
    interaction: ChatPanel
    ---
    ```user```
    
    你是一个资深的软件开发工程师，你擅长使用 TDD 的方式来开发软件，你需要根据新的测试用例，来改进原有的代码实现。
    
    原有的实现代码是：$context.underTestFileCode($methodName)
    
    $context.underTestMethodCode($methodName)
    
    新的测试代码是：
    
    ${selection}
    
    请根据新的测试，优化 class under test 部分的代码。请返回对应的方法的代码，使用 ``` 开始你的代码块：
    
