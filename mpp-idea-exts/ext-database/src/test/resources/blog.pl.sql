CREATE OR REPLACE PROCEDURE find_blog_by_id(
    p_blog_id IN NUMBER,
    p_blog_content OUT VARCHAR2
) AS
BEGIN
    SELECT blog_content
    INTO p_blog_content
    FROM blogs
    WHERE blog_id = p_blog_id;

EXCEPTION
    WHEN NO_DATA_FOUND THEN
        p_blog_content := NULL; -- 或者你可以选择处理其他错误
END find_blog_by_id;
/
