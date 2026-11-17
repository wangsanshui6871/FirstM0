package com.example.firstm0.llm;

/**
 * 发给大模型的一条消息。
 *
 * <p>OpenAI 兼容接口的消息格式就是两段文字：
 * <pre>
 * { "role": "system", "content": "..." }
 * { "role": "user", "content": "..." }
 * </pre>
 * role 只有三种：
 * <ul>
 *   <li>system：给模型的规则，比如“你是客服助手”</li>
 *   <li>user：用户说的话</li>
 *   <li>assistant：模型之前的回复</li>
 * </ul>
 *
 * <p>为什么不直接用 domain 包的 Message 实体？
 * 因为实体属于数据库层，字段是 conversationId、createdAt 这些；
 * 而这里只关心 role 和 content，两者职责不同，分开更清楚。
 *
 * <p>提供三个静态工厂方法，避免调用方手写 "system"、"user" 这类字符串时不慎拼错。
 */
public record LlmMessage(String role, String content) {

    public static LlmMessage system(String content) {
        return new LlmMessage("system", content);
    }

    public static LlmMessage user(String content) {
        return new LlmMessage("user", content);
    }

    public static LlmMessage assistant(String content) {
        return new LlmMessage("assistant", content);
    }
}
