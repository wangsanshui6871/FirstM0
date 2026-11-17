package com.example.firstm0.llm;

import java.util.List;
import java.util.function.Consumer;

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
 * <p>注意：接口里不出现 RestClient、HTTP 状态码这些细节，
 * 那些都属于实现类内部的事。
 */
public interface LlmClient {

    /**
     * 一次性拿到完整回复（非流式）。
     *
     * @param messages 按时间顺序排列的消息，通常第一条是 system，最后一条是用户当前问题
     * @return 模型回复的文本，保证非空白
     * @throws LlmException 未配置 Key、超时、HTTP 报错、返回为空时抛出
     */
    String chat(List<LlmMessage> messages);

    /**
     * 流式获取回复：每收到一小段就回调一次，最后返回完整文本。
     *
     * <p>为什么要流式：模型生成一段 200 字的回答可能要 3 秒，
     * 非流式就是“等 3 秒 → 整段出现”，用户以为卡死了；
     * 流式是“0.5 秒就开始出字”，体验完全不同。
     *
     * <p>回调里抛出的异常（比如客户端断开导致写不出去）会中断读取，
     * 由调用方决定怎么处理。
     *
     * @param messages 完整消息列表
     * @param onDelta  每收到一个片段就调用一次，参数是这一小段文字
     * @return 拼接好的完整回复
     * @throws LlmException 未配置 Key、HTTP 报错、返回为空时抛出
     */
    String chatStream(List<LlmMessage> messages, Consumer<String> onDelta);
}
