package com.example.firstm0.llm;

import java.util.List;

/**
 * 大模型调用的抽象。
 *
 * <p>为什么这里要定义一个接口，而不是直接在 Service 里调 HTTP？
 * <ul>
 *   <li>Service 只关心“把这些消息发给模型、拿回一句回复”，不关心是 DeepSeek 还是别的厂商；</li>
 *   <li>测试时可以换成假的实现／Mock，不用真的发网络请求、不花钱；</li>
 *   <li>以后换厂商，只改实现类，Service 一行都不用动。</li>
 * </ul>
 *
 * <p>M0.4 起参数从“一句话”改成“一组消息”，
 * 这样就能把 system prompt、历史对话、当前问题一起发过去。
 *
 * <p>注意：接口里不出现 RestClient、HTTP 状态码这些细节，
 * 那些都属于实现类内部的事。
 */
public interface LlmClient {

    /**
     * 让模型根据完整的消息列表生成回复。
     *
     * @param messages 按时间顺序排列的消息，通常第一条是 system，最后一条是用户当前问题
     * @return 模型回复的文本，保证非空白
     * @throws LlmException 未配置 Key、超时、HTTP 报错、返回为空时抛出
     */
    String chat(List<LlmMessage> messages);
}
