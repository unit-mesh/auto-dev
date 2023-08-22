Write a code snippet that could hypothetically be returned by a code search engine as the answer to the query: {query}

- Write the snippets in a programming or markup language that is likely given the query
- The snippet should be between 5 and 10 lines long
- Surround the snippet in triple backticks

For example:

What's the Qdrant threshold?

```rust
SearchPoints {{
    limit,
    vector: vectors.get(idx).unwrap().clone(),
    collection_name: COLLECTION_NAME.to_string(),
    offset: Some(offset),
    score_threshold: Some(0.3),
    with_payload: Some(WithPayloadSelector {{
        selector_options: Some(with_payload_selector::SelectorOptions::Enable(true)),
    }}),
```
