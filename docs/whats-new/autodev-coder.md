---
layout: default
title: AutoDev Coder
nav_order: 4
parent: What's New
---

太长不读性：

适用于 AutoDev 的编码大模型 AutoDev Coder 6.7B 第一个**勉强可用**的版本出来的。

- HuggingFace 首页：[https://huggingface.co/unit-mesh](https://huggingface.co/unit-mesh/autodev-coder) （暂时没有资质提供直接下载，🐶🐶）。
- 数据集下载地址：https://huggingface.co/datasets/unit-mesh/autodev-datasets

PS：由于 AutoDev 1.5.1 在 JetBrains 市场等待审批，而老外们正在休完假，所以模型在 1.5.1 上的体验会比 1.5.0 **略微**好一点。

除此，在有了更好的算力支持，经过更好的补全测试之后，我们也会将原来的 Inlay 补全模式加回来。

## AutoDev Coder 6.7B v1 试验版

当前版本基于 LLaMA 架构下的 DeepSeek Coder 6.7b instruct 模型微调的。

注意事项：作为试验版，主要是为了磨合模型、数据工具与 IDE 插件，以达成更好的协调。因此，在生成质量还需要进一步提高。

## AutoDev Coder 64k 数据集

如下是 AutoDev Coder v1 64k 的指令组成：

| 文件名                                    | 选取的指令数 |
|----------------------------------------|--------|
| java_oss.jsonl                         | 4000   |
| python_oss.jsonl                       | 4000   |
| code_bugfix_cleaned_5K.json            | 4000   |
| codeGPT_CN_cleaned_20K.json            | 15000  |
| code_summarization_CN_cleaned_10K.json | 8000   |
| code_generation_CN_cleaned_5K.json     | 4000   |
| summary.jsonl                          | 25000  |

其中的 summary.jsonl 是由我们开源的代码微调数据框架 UnitGen 生成（https://github.com/unit-mesh/unit-gen）。

我们挑选了几十个开源软件 Java 和 Kotlin 语言，根据 AutoDev 插件的指令生成，主要分为三类：

- 补全（行内、行间、块间）
- 文档生成
- 注释生成

详细说明可以见 UnitGen 项目和文档：https://github.com/unit-mesh/unit-gen。

## FAQ：AutoDev Coder 模型评估

暂时还在设计中。由于我们需要结合 AutoDev 指令与不同的语言如 Java、 Kotlin 、TypeScript 等语言，而非各种开源模型中喜欢用的 Python 体系，所以需要重新思考怎么设计。

我们前期采用 OSS Instruct 等指令集作为自然语言生成代码的补充，后来发现有一半的指令（～50，000 ）与 Python 相关，后来从中刷选出 Java 大概在 ~5，000 左右。在 AutoDev 中采用结果并不是很好。

## FAQ：AutoDev 指令

AutoDev 采用的是相关上下文策略，所以在指令上与其它工具有所差异。详细见：https://github.com/unit-mesh/auto-dev
