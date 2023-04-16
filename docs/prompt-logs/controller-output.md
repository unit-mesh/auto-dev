# Output


```
@PostMapping("/blog")
public ResponseEntity<?> createBlog(@RequestBody Blog blog) {
try {
// 验证博客信息是否完整
if (StringUtils.isBlank(blog.getTitle())) {
return ResponseEntity.badRequest().body("请输入博客标题");
}

        // 调用 BlogService 创建博客
        Long blogId = blogService.createBlog(blog);

        // 返回博客 id
        return ResponseEntity.ok(blogId);
    } catch (Exception e) {
        // 返回错误信息
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("创建博客失败，请稍后重试");
    }
}
```

