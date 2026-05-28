package com.huang.zhixing.controller;

import com.huang.zhixing.common.Result;
import com.huang.zhixing.model.entity.BiChatMessage;
import com.huang.zhixing.model.entity.BiChatSession;
import com.huang.zhixing.service.ChatSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/session")
@RequiredArgsConstructor
public class ChatSessionController {

    private final ChatSessionService sessionService;

    /**
     * 创建新会话
     */
    @PostMapping("/create")
    public Result<Map<String, String>> create(@RequestParam("userId") String userId,
                                               @RequestParam("title") String title) {
        String sessionId = sessionService.createSession(userId, title);
        return Result.success(Map.of("sessionId", sessionId));
    }

    /**
     * 获取用户的所有会话列表（按活跃时间倒序）
     */
    @GetMapping("/list")
    public Result<List<BiChatSession>> list(@RequestParam("userId") String userId) {
        List<BiChatSession> sessions = sessionService.listSessions(userId);
        return Result.success(sessions);
    }

    /**
     * 获取会话全量消息（含安全校验）
     */
    @GetMapping("/history")
    public Result<List<BiChatMessage>> history(@RequestParam("userId") String userId,
                                                @RequestParam("sessionId") String sessionId) {
        List<BiChatMessage> messages = sessionService.getRawChatHistory(userId, sessionId);
        return Result.success(messages);
    }

    /**
     * 清空会话消息（含安全校验）
     */
    @DeleteMapping("/clear")
    public Result<?> clear(@RequestParam("userId") String userId,
                           @RequestParam("sessionId") String sessionId) {
        sessionService.clearMessages(userId, sessionId);
        return Result.success();
    }
}
