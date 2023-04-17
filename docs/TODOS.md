## Working Process

1. Generate Story from AI
2. create API from Story
3. send package information with call chain
4. Analysis call chain, auto update code
5. generate code with AutoComplete

Index in Server side.

instruction: 

- `devti://code/completion`
- `devti://code/check-bug`

Use CLI to verify

```java
@Controller
@RequestMapping("/api")
class ApiController {
    @Autowired
    private ApiService apiService;

    @GetMapping("/story")
    public ResponseEntity<Story> getStory(@RequestParam("id") String id) {
        return ResponseEntity.ok(apiService.getStory(id));
    }

    @PostMapping("/story")
    public ResponseEntity<Story> createStory(@RequestBody Story story) {
        return ResponseEntity.ok(apiService.createStory(story));
    }

    @PutMapping("/story")
    public ResponseEntity<Story> updateStory(@RequestBody Story story) {
        return ResponseEntity.ok(apiService.updateStory(story));
    }

    @DeleteMapping("/story")
    public ResponseEntity<Story> deleteStory(@RequestParam("id") String id) {
        return ResponseEntity.ok(apiService.deleteStory(id));
    }
}
```

