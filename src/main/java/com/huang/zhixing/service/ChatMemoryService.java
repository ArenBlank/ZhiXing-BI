package com.huang.zhixing.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huang.zhixing.mapper.BiChatMessageMapper;
import com.huang.zhixing.mapper.BiChatSessionMapper;
import com.huang.zhixing.model.entity.BiChatMessage;
import com.huang.zhixing.model.entity.BiChatSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMemoryService {

    private final BiChatSessionMapper sessionMapper;
    private final BiChatMessageMapper messageMapper;

    @Value("${zhixing.memory.max-messages:10}")
    private int defaultMaxMessages;

    private static final String SYSTEM_PROMPT = """
            你是 ZhiXing-BI，一个友好、知识渊博的智能助手。

            回复时请遵循：
            1. 开头用一两句话交代用了什么工具、拿到了什么数据。
            2. 大段之间必须用空行分隔，不要用 --- 横线。
            3. 每个大段的标题用**粗体**，如**第一，xxxx**。
            4. 绝对禁止 Markdown 表格（|--|--|）和 ## 标题。
            5. 段落内不要堆砌，每写完一个观点换行。

            当用户询问天气、新闻、最新数据时，主动调用 webSearch。
            """;

    /**
     * 滑动窗口记忆加载：逆序查库 → 内存翻转 → 角色映射 → SystemMessage 前置
     *
     * @param maxMessages 窗口大小（最近 N 条）
     * @return 时间正序的 Spring AI Message 列表，首位为 SystemMessage
     */
    public List<Message> loadSlidingWindowHistory(String userId, String sessionId, int maxMessages) {
        // 1. 防越权校验
        BiChatSession session = sessionMapper.selectOne(
                new LambdaQueryWrapper<BiChatSession>()
                        .eq(BiChatSession::getSessionId, sessionId)
        );
        if (session == null) {
            throw new IllegalArgumentException("会话不存在: " + sessionId);
        }
        if (!session.getUserId().equals(userId)) {
            log.warn("越权拦截（memory）: userId={} 试图加载 sessionId={}", userId, sessionId);
            throw new IllegalArgumentException("无权访问该会话");
        }

        // 2. DESC LIMIT 查询 —— MySQL 只返回需要的行，不浪费带宽
        List<BiChatMessage> descMessages = messageMapper.selectList(
                new LambdaQueryWrapper<BiChatMessage>()
                        .eq(BiChatMessage::getSessionId, sessionId)
                        .orderByDesc(BiChatMessage::getCreateTime)
                        .last("LIMIT " + Math.max(1, maxMessages))
        );

        // 3. 内存翻转恢复时间正序
        Collections.reverse(descMessages);

        // 4. 角色映射 → Spring AI Message
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(SYSTEM_PROMPT));

        for (BiChatMessage msg : descMessages) {
            messages.add(mapToSpringAiMessage(msg));
        }

        log.info("滑动窗口加载: sessionId={}, total={}, window={}",
                sessionId, descMessages.size(), maxMessages);
        return messages;
    }

    /**
     * 便捷方法：使用默认窗口大小
     */
    public List<Message> loadSlidingWindowHistory(String userId, String sessionId) {
        return loadSlidingWindowHistory(userId, sessionId, defaultMaxMessages);
    }

    /**
     * 构建带 RAG 知识注入的增强用户消息
     *
     * @param userPrompt 用户原始提问
     * @param ragContext RAG 检索到的知识片段（可能为空）
     * @return 融合了背景知识的 UserMessage
     */
    public UserMessage buildAugmentedUserMessage(String userPrompt, List<org.springframework.ai.document.Document> ragContext) {
        if (ragContext == null || ragContext.isEmpty()) {
            return new UserMessage(userPrompt);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("【以下是从你的知识库中检索到的相关背景资料】\n\n");
        for (int i = 0; i < ragContext.size(); i++) {
            sb.append("--- 参考资料 ").append(i + 1).append(" ---\n");
            sb.append(ragContext.get(i).getContent()).append("\n\n");
        }
        sb.append("【基于以上参考资料，请回答用户的问题】\n");
        sb.append(userPrompt);

        return new UserMessage(sb.toString());
    }

    /**
     * BiChatMessage → Spring AI Message 角色映射
     */
    private Message mapToSpringAiMessage(BiChatMessage msg) {
        return switch (msg.getRole().toLowerCase()) {
            case "user" -> new UserMessage(msg.getContent());
            case "assistant" -> new AssistantMessage(msg.getContent());
            case "system" -> new SystemMessage(msg.getContent());
            default -> {
                log.warn("未知消息角色: {}, 降级为 UserMessage", msg.getRole());
                yield new UserMessage(msg.getContent());
            }
        };
    }
}
