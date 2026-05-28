package com.huang.zhixing.controller;

import com.huang.zhixing.common.Result;
import com.huang.zhixing.common.UserContext;
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

    @PostMapping("/create")
    public Result<Map<String, String>> create(@RequestParam("title") String title) {
        String userId = UserContext.get();
        String sessionId = sessionService.createSession(userId, title);
        return Result.success(Map.of("sessionId", sessionId));
    }

    @GetMapping("/list")
    public Result<List<BiChatSession>> list() {
        return Result.success(sessionService.listSessions(UserContext.get()));
    }

    @GetMapping("/history")
    public Result<List<BiChatMessage>> history(@RequestParam("sessionId") String sessionId) {
        return Result.success(sessionService.getRawChatHistory(UserContext.get(), sessionId));
    }

    @DeleteMapping("/clear")
    public Result<?> clear(@RequestParam("sessionId") String sessionId) {
        sessionService.clearMessages(UserContext.get(), sessionId);
        return Result.success();
    }

    @DeleteMapping("/delete")
    public Result<?> delete(@RequestParam("sessionId") String sessionId) {
        sessionService.deleteSession(UserContext.get(), sessionId);
        return Result.success();
    }
}
