package com.example.firstm0.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 聊天业务逻辑。
 *
 * <p>为什么需要 Service 层：Controller 只负责“接请求、返响应”，
 * 真正的业务处理（以后是存消息、查历史、调大模型、拼上下文）都放在这里。
 * 这样 Controller 保持很薄，业务改动不会散落在接口代码里。
 *
 * <p>M0.1 阶段这里只做一件事：把用户输入原样“收到”回去，证明整条链路通了。
 * M0.2 会在这里加数据库，M0.3 会在这里换成真实大模型调用。
 */
@Service  // 告诉 Spring：把这个类创建成 Bean，交给容器管理（Controller 才能注入它）
public class ChatService {

    // SLF4J 日志：项目规范要求用日志而不是 System.out.println
    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    /**
     * 生成回复。
     *
     * <p>M0.1 是固定格式的假回复；M0.3 会把这里替换成调用大模型。
     *
     * @param message 用户输入的内容（已经过 Controller 的校验，这里可以放心使用）
     * @return 固定格式的回复字符串
     */
    public String generateReply(String message) {
        // 记录一条日志，方便排查“请求到底有没有进到业务层”
        log.info("收到用户消息，长度={}", message.length());

        return "我收到了：" + message;
    }
}
