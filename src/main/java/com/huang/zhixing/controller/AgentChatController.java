package com.huang.zhixing.controller;

import com.huang.zhixing.common.AgentAudit;
import com.huang.zhixing.common.Result;
import com.huang.zhixing.common.UserContext;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

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

    @AgentAudit
    @PostMapping("/chat")
    public Result<Map<String, String>> chat(
            @RequestParam("sessionId") String sessionId,
            @RequestParam("userPrompt") String userPrompt) {
        String userId = UserContext.get();

        List<Message> memory = chatMemoryService.loadSlidingWindowHistory(userId, sessionId);
        List<Document> ragDocs = vectorStorageService.searchUserKnowledge(userPrompt, userId, sessionId, 3);
        Message augmentedMsg = chatMemoryService.buildAugmentedUserMessage(userPrompt, ragDocs);
        memory.add(augmentedMsg);

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .withModel("deepseek-chat")
                .withFunctionCallbacks(toolCallbacks)
                .build();

        Prompt prompt = new Prompt(memory, options);
        ChatResponse response = chatModel.call(prompt);
        String aiContent = response.getResult().getOutput().getContent();

        chatSessionService.saveMessage(userId, sessionId, "user", userPrompt);
        chatSessionService.saveMessage(userId, sessionId, "assistant", aiContent);

        return Result.success(Map.of("response", aiContent));
    }
}
