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

            回复格式要求（非常重要）：
            你的每条回复都必须分为两个部分——
            **思考过程**
            用几句话简述你的分析思路：你调用了什么工具、拿到了什么数据、准备从哪几个角度展开。
            这部分要简洁，3-5行为宜。
            ---
            **回答**
            这是给用户的正式回复。用自然段落叙述，绝对禁止 Markdown 表格（|--|--|）和 ## 标题。
            每个观点都要展开，讲清楚逻辑和影响。
            用"首先…其次…最后"组织逻辑。
            多轮对话要主动引用之前聊过的内容。
            可以适当使用**加粗**和数字编号。

            关于本平台界面：输入框上方有一个"○ 联网搜索"切换按钮，
            点击后变成"✅ 联网搜索"，消息会触发实时网络搜索。

            当用户询问天气、新闻、最新数据等实时信息时，主动调用 webSearch。
            当用户上传文件时，帮他们深度分析内容。
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
