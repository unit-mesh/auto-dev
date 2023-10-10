---
layout: default
title: Prompt Strategy
nav_order: 98
permalink: /strategy/prompt
---

# Prompt Strategy

simliar to JetBrains LLM and GitHub Copilot, will be implementation like this:

```javascript
defaultPriorities.json = [
    "BeforeCursor",
    "SimilarFile",
    "ImportedFile",
    "PathMarker",
    "LanguageMarker"
]
```

We currently support:

- [x] BeforeCursor
- [ ] SimilarFile
    - [x] JaccardSimilarity Path and Chunks by JetBrains
    - [ ] Cosine Similarity Chunk by MethodName
- [ ] ImportedFile
    - [x] Java, Kotlin
    - [ ] all cases
- [x] PathMarker
- [x] LanguageMarker
