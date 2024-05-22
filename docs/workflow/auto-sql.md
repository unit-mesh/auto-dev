---
layout: default
title: AutoSQL
nav_order: 2
parent: Workflow
---

Required Intellij Plugin:

Database Tools and SQL
{: .label .label-yellow }

Demo Video: [https://www.bilibili.com/video/BV1Ye411h7Qu/](https://www.bilibili.com/video/BV1Ye411h7Qu/)

implementation: cc.unitmesh.database.flow.AutoSqlFlow

1. user should connect to DataSource/Database
2. based on user input, AI select the target tables.
3. generate the SQL scripts for the target tables.
    - generate columns of selected tables.
    - generate SQL scripts for selected tables.

## Prompt Override

Steps:

- step 1: `prompts/genius/sql/sql-gen-clarify.vm`
- step 2: `prompts/genius/sql/sql-gen-design.vm`

Context:

```kotlin
data class AutoSqlContext(
   val requirement: String,
   val databaseVersion: String,
   val schemaName: String,
   val tableNames: List<String>,
   /**
    * Step 2.
    * A list of table names to retrieve the columns from.
    */
   var tableInfos: List<String> = emptyList(),
)
```

### Current Prompt

Clarify:

    You are a professional Database Administrator.
    According to the user's requirements, you should choose the best Tables for the user in List.
    
    — User use database: ${context.databaseVersion}
    - User schema name: ${context.schemaName}
    - User tables: ${context.tableNames}
    
    For example:
    
    - Question(requirements): calculate the average trip length by subscriber type.// User tables: trips, users, subscriber_type
    - You should anwser: [trips, subscriber_type]
    
    ----
    
    Here are the User requirements:
    
    ```markdown
    ${context.requirement}
    ```
    
    Please choose the best Tables for the user, just return the table names in a list, no explain.
    
Design:
    
    You are a professional Database Administrator.
    According to the user's requirements, and Tables info, write SQL for the user.
    
    — User use database: ${context.databaseVersion}
    - User schema name: ${context.schemaName}
    - User tableInfos: ${context.tableInfos}
    
    For example:
    
    - Question(requirements): calculate the average trip length by subscriber type.
    // table `subscriber_type`: average_trip_length: int, subscriber_type: string
    - Answer:
    ```sql
    select average_trip_length from subscriber_type where subscriber_type = 'subscriber'
    ```
    
    ----
    
    Here are the requirements:
    
    ```markdown
    ${context.requirement}
    ```
    
    Please write your SQL with Markdown syntax, no explanation is needed. :
    
    
