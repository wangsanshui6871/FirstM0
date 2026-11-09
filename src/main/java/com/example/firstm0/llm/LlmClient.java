package com.example.firstm0.llm;

/**
 * 大模型调用的抽象。
 *
 * <p>为什么这里要定义一个接口，而不是直接在 Service 里调 HTTP？
 * <ul>
 *   <li>Service 只关心“给我一句回复”，不关心是 DeepSeek、通义还是本地模型；</li>
 *   <li>测试时可以换成一个假的实现／Mock，不用真的发网络请求、不花钱；</li>
 *   <li>以后换厂商，只改实现类，Service 一行都不用动。</li>
 * </ul>
 *
 * <p>注意：接口里不出现 RestClient、HTTP 状态码这些细节，
 * 那些都属于实现类内部的事。
 */
public interface LlmClient {

    /**
     * 让模型回答一句用户消息。
     *
     * @param systemPrompt 系统提示词，用来规定“你是谁、该怎么回答”
     * @param userMessage  用户这次说的话
     * @return 模型回复的文本，保证非空白
     * @throws LlmException 未配置 Key、超时、HTTP 报错、返回为空时抛出
     */
    String chat(String systemPrompt, String userMessage);
}
