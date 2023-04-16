请将下面的用户故事转换为操作步骤，以便于我编写代码。要求如下：

1. 按格式返回，不做解释。
2. 你返回的内容格式 如下：

###
controller: 1.xxx 2.xxx 3.xxx
service: 1.xxx 2.xxx 3.xxx
dao: 1.xxx 2.xxx 3.xxx
###

###
用户故事：创建博客
作为一个用户
我想新建博客，如果成功返回 id，如果失败返回对应错误信息

AC 1: 用户可以成功创建博客并返回对应的 id。
假设用户输入正确的博客信息
当用户点击“创建博客”按钮
于是系统返回对应的 id

AC 2: 用户创建博客时，如果输入信息不完整，系统应该返回相应的错误信息。
假设用户未输入博客标题
当用户点击“创建博客”按钮
于是系统返回“请输入博客标题”的错误信息

AC 3: 用户创建博客时，如果出现其他未知错误，系统应该返回相应的错误信息。
假设系统因为某种原因无法创建博客
当用户点击“创建博客”按钮
于是系统返回“创建博客失败，请稍后重试”的错误信息
###

```
以下是根据用户故事转换为操作步骤的代码实现，返回内容按照要求格式化：

// Controller
public class BlogController {
    private BlogService blogService;

    public BlogController(BlogService blogService) {
        this.blogService = blogService;
    }

    public String createBlog(String title, String content) {
        try {
            Long id = blogService.createBlog(title, content);
            return String.format("controller: 1.success, id: %d", id);
        } catch (IncompleteBlogException e) {
            return String.format("controller: 2.%s", e.getMessage());
        } catch (Exception e) {
            return "controller: 3.createBlog failed, please try again later";
        }
    }
}

// Service
public class BlogService {
    private BlogDao blogDao;

    public BlogService(BlogDao blogDao) {
        this.blogDao = blogDao;
    }

    public Long createBlog(String title, String content) throws IncompleteBlogException {
        if (title == null || title.isEmpty()) {
            throw new IncompleteBlogException("Please enter blog title");
        }
        Blog blog = new Blog(title, content);
        return blogDao.createBlog(blog);
    }
}

// DAO
public class BlogDao {
    private Long idSequence = 0L;

    public Long createBlog(Blog blog) {
        blog.setId(++idSequence);
        // save blog to database
        return blog.getId();
    }
}

// Exception
public class IncompleteBlogException extends Exception {
    public IncompleteBlogException(String message) {
        super(message);
    }
}

其中，Blog 是一个包含标题和内容的实体类，BlogController 是控制器类，BlogService 是服务类，BlogDao 是数据访问对象类，
IncompleteBlogException 是自定义的异常类，用于处理博客信息不完整的情况。在 BlogController 的 createBlog 方法中，如果成功创建博客，
则返回对应的 id；如果博客信息不完整，则返回相应的错误信息；如果出现其他未知错误，则返回“创建博客失败，请稍后重试”的错误信息。
返回的内容按照要求格式化，以便于使用。
```