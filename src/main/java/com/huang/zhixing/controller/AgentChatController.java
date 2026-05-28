package com.huang.zhixing.controller;

import com.huang.zhixing.common.AgentAudit;
import com.huang.zhixing.common.Result;
import com.huang.zhixing.service.ChatMemoryService;
import com.huang.zhixing.service.ChatSessionService;
import com.huang.zhixing.service.VectorStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.beans.factory.annotation.Qualifier;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/agent")
public class AgentChatController {

    private final ChatMemoryService chatMemoryService;
    private final VectorStorageService vectorStorageService;
    private final ChatSessionService chatSessionService;
    private final ChatModel chatModel;
    private final List<FunctionCallback> toolCallbacks;

    public AgentChatController(ChatMemoryService chatMemoryService,
                               VectorStorageService vectorStorageService,
                               ChatSessionService chatSessionService,
                               @Qualifier("openAiChatModel") ChatModel chatModel,
                               List<FunctionCallback> toolCallbacks) {
        this.chatMemoryService = chatMemoryService;
        this.vectorStorageService = vectorStorageService;
        this.chatSessionService = chatSessionService;
        this.chatModel = chatModel;
        this.toolCallbacks = toolCallbacks;
    }

    /**
     * Agent 核心对话接口 —— 记忆 + RAG + 工具链 ReAct 三库联动
     *
     * 大模型在本接口中具备：
     *   感知：读懂用户提问，判断是否需要时间/搜索辅助
     *   思考：决定调用哪个工具
     *   行动：Spring AI 自动拦截 tool_calls → 执行工具 → 回传结果
     *   验证：基于多路数据（MySQL记忆 + Redis知识库 + 工具结果）产出方案
     */
    @AgentAudit
    @PostMapping("/chat")
    public Result<Map<String, String>> chat(
            @RequestParam("userId") String userId,
            @RequestParam("sessionId") String sessionId,
            @RequestParam("userPrompt") String userPrompt) {

        // 1. 加载滑动窗口记忆
        List<Message> memory = chatMemoryService.loadSlidingWindowHistory(userId, sessionId);

        // 2. Redis-Stack 向量库检索
        List<Document> ragDocs = vectorStorageService.searchUserKnowledge(
                userPrompt, userId, sessionId, 3);

        // 3. 构建 RAG 增强用户消息（空结果时降级为原始提问）
        Message augmentedMsg = chatMemoryService.buildAugmentedUserMessage(userPrompt, ragDocs);
        memory.add(augmentedMsg);

        // 4. 绑定工具链，构建带工具能力的 Prompt
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .withModel("deepseek-chat")
                .withFunctionCallbacks(toolCallbacks)
                .build();

        Prompt prompt = new Prompt(memory, options);
        log.info("Agent 发起调用: userId={}, sessionId={}, 工具数量={}, ragHits={}",
                userId, sessionId, toolCallbacks.size(), ragDocs.size());

        // 5. 调用 DeepSeek —— Spring AI 自动处理 ReAct 工具调用循环
        ChatResponse response = chatModel.call(prompt);
        String aiContent = response.getResult().getOutput().getContent();

        // 6. 持久化本轮对话
        chatSessionService.saveMessage(userId, sessionId, "user", userPrompt);
        chatSessionService.saveMessage(userId, sessionId, "assistant", aiContent);

        log.info("Agent 对话完成: userId={}, sessionId={}", userId, sessionId);
        return Result.success(Map.of("response", aiContent));
    }
}
