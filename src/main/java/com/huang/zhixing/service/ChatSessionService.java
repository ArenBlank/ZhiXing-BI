package com.huang.zhixing.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huang.zhixing.mapper.BiChatMessageMapper;
import com.huang.zhixing.mapper.BiChatSessionMapper;
import com.huang.zhixing.model.entity.BiChatMessage;
import com.huang.zhixing.model.entity.BiChatSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatSessionService {

    private final BiChatSessionMapper sessionMapper;
    private final BiChatMessageMapper messageMapper;

    /**
     * 创建新会话
     */
    public String createSession(String userId, String title) {
        BiChatSession session = new BiChatSession();
        session.setSessionId(UUID.randomUUID().toString().replace("-", ""));
        session.setUserId(userId);
        session.setTitle(title);
        sessionMapper.insert(session);
        log.info("会话创建: userId={}, sessionId={}, title={}", userId, session.getSessionId(), title);
        return session.getSessionId();
    }

    /**
     * 获取用户的所有会话（按最近活跃倒序）
     */
    public List<BiChatSession> listSessions(String userId) {
        return sessionMapper.selectList(
                new LambdaQueryWrapper<BiChatSession>()
                        .eq(BiChatSession::getUserId, userId)
                        .orderByDesc(BiChatSession::getUpdateTime)
        );
    }

    /**
     * 安全校验 + 获取会话全量消息（时间正序）
     *
     * 防越权核心：先查 bi_chat_session 校验 sessionId 归属，
     * 不匹配则直接抛出 IllegalArgumentException，绝不返回他人数据
     */
    public List<BiChatMessage> getRawChatHistory(String userId, String sessionId) {
        BiChatSession session = sessionMapper.selectOne(
                new LambdaQueryWrapper<BiChatSession>()
                        .eq(BiChatSession::getSessionId, sessionId)
        );

        if (session == null) {
            throw new IllegalArgumentException("会话不存在: " + sessionId);
        }
        if (!session.getUserId().equals(userId)) {
            log.warn("越权拦截: userId={} 试图访问 sessionId={}（实际归属 userId={}）",
                    userId, sessionId, session.getUserId());
            throw new IllegalArgumentException("无权访问该会话");
        }

        return messageMapper.selectList(
                new LambdaQueryWrapper<BiChatMessage>()
                        .eq(BiChatMessage::getSessionId, sessionId)
                        .orderByAsc(BiChatMessage::getCreateTime)
        );
    }

    /**
     * 保存单条消息（含归属校验，防横向越权写）
     */
    public void saveMessage(String userId, String sessionId, String role, String content) {
        BiChatSession session = sessionMapper.selectOne(
                new LambdaQueryWrapper<BiChatSession>()
                        .eq(BiChatSession::getSessionId, sessionId)
        );

        if (session == null) {
            throw new IllegalArgumentException("会话不存在: " + sessionId);
        }
        if (!session.getUserId().equals(userId)) {
            log.warn("越权拦截（save）: userId={} 试图写入 sessionId={}", userId, sessionId);
            throw new IllegalArgumentException("无权操作该会话");
        }

        BiChatMessage message = new BiChatMessage();
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContent(content);
        messageMapper.insert(message);

        // 首次用户消息 → 用提问内容自动生成会话标题
        if ("user".equals(role) && "新会话".equals(session.getTitle())) {
            String title = content.length() > 30 ? content.substring(0, 30) : content;
            title = title.replace("\n", " ").trim();
            session.setTitle(title);
            sessionMapper.updateById(session);
        }
    }

    /**
     * 安全校验后删除会话及所有消息
     */
    public void deleteSession(String userId, String sessionId) {
        BiChatSession session = sessionMapper.selectOne(
                new LambdaQueryWrapper<BiChatSession>()
                        .eq(BiChatSession::getSessionId, sessionId)
        );

        if (session == null) {
            throw new IllegalArgumentException("会话不存在: " + sessionId);
        }
        if (!session.getUserId().equals(userId)) {
            log.warn("越权拦截（delete）: userId={} 试图删除 sessionId={}", userId, sessionId);
            throw new IllegalArgumentException("无权操作该会话");
        }

        messageMapper.delete(
                new LambdaQueryWrapper<BiChatMessage>()
                        .eq(BiChatMessage::getSessionId, sessionId)
        );
        sessionMapper.deleteById(session.getId());
        log.info("会话已删除: sessionId={}, userId={}", sessionId, userId);
    }

    /**
     * 安全校验后清空会话的所有消息
     */
    public void clearMessages(String userId, String sessionId) {
        BiChatSession session = sessionMapper.selectOne(
                new LambdaQueryWrapper<BiChatSession>()
                        .eq(BiChatSession::getSessionId, sessionId)
        );

        if (session == null) {
            throw new IllegalArgumentException("会话不存在: " + sessionId);
        }
        if (!session.getUserId().equals(userId)) {
            log.warn("越权拦截（clear）: userId={} 试图清空 sessionId={}", userId, sessionId);
            throw new IllegalArgumentException("无权操作该会话");
        }

        messageMapper.delete(
                new LambdaQueryWrapper<BiChatMessage>()
                        .eq(BiChatMessage::getSessionId, sessionId)
        );
        log.info("会话消息已清空: sessionId={}, userId={}", sessionId, userId);
    }
}
