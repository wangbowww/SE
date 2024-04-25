package net.micode.notes.gtask.data;

// 导入必要的库
import java.io.IOException;

// 自定义内容解析异常类
public class ContentParsingException extends IOException {

    // 默认构造函数
    public ContentParsingException() {
        super();
    }

    // 带有消息参数的构造函数
    public ContentParsingException(String message) {
        super(message);
    }

    // 带有消息和原因参数的构造函数
    public ContentParsingException(String message, Throwable cause) {
        super(message, cause);
    }

}