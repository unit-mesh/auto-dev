---
layout: default
title: Refactoring Prompt
nav_order: 13
parent: Development
---

## Refactoring

User: 现在的代码逻辑不够概念化，理解起来有点困难
Assistant:你的问题是当前的 DirInsCommand 类逻辑不够概念化，理解起来比较困难。我将通过重构来提高代码的可读性和概念清晰度。

解决步骤：
- 引入更明确的数据模型来表示目录结构
- 分离展示逻辑和数据收集逻辑
- 简化递归遍历算法
- 提高命名的表达性
- 添加更多注释说明设计思想

