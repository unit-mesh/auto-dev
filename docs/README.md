# RoadMap

## LLM as Copilot

AutoMode

- [x] AutoCrud
- [x] AutoTest
- [ ] AutoDoc
- [x] AutoComplete

ChatMode

- [x] Chat panel
- [x] Chat With this

Copilot Mode

- [x] Gen commit message
- [x] Gen changelog
- [x] Refactor code
- [x] Code complete with context
- [x] Fix error

## LLM as Co-Integrator (Draft)

### API design/integrator chat

Supply APIs:

- Unique Language
    - 统一语言层面的表达，消除对于 LLM 的歧义。
    - 术语表补充
    - 语言翻译：JB -> JetBrains, J8 -> Java 8, HFD -> 还房贷、黄峰达、Hadoop File System
- Business Logic
    - 某个业务逻辑是如何实现的？
    - 生成用例
- Supply API
    - 某个外部的 API 修改，是否会对该服务造成影响？
    - 要实现 xx 功能，从数据库表，到 API，需要哪些改动？
- Database Schema for Chat
    - 表结构与领域模型是否合理？
    - 变更影响分析

### Business knowledge chat


