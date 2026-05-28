package com.huang.zhixing.common;

import com.huang.zhixing.mapper.BiAgentAuditLogMapper;
import com.huang.zhixing.model.entity.BiAgentAuditLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AgentAuditAspect {

    private final BiAgentAuditLogMapper auditLogMapper;

    /**
     * 环绕通知 —— 拦截所有标注 @AgentAudit 的方法
     *
     * 生命周期：
     *   1. 反射提取 userId / sessionId / userPrompt
     *   2. 记录开始时间戳
     *   3. 放行方法 → 执行大模型 ReAct 推理链
     *   4. 计算耗时 + 提取 AI 响应
     *   5. 异步持久化审计日志到 MySQL
     */
    @Around("@annotation(agentAudit)")
    public Object audit(ProceedingJoinPoint joinPoint, AgentAudit agentAudit) throws Throwable {
        // 1. 反射提取入参 —— 不依赖参数顺序，只认参数名
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        String userId = null, sessionId = null, userPrompt = null;
        for (int i = 0; i < paramNames.length; i++) {
            switch (paramNames[i]) {
                case "userId" -> userId = (String) args[i];
                case "sessionId" -> sessionId = (String) args[i];
                case "userPrompt" -> userPrompt = (String) args[i];
            }
        }

        // 2. 计时开始
        long startTime = System.currentTimeMillis();

        // 3. 放行 → 真正执行 Controller 方法（MySQL记忆 + Redis RAG + ReAct 工具链）
        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Throwable e) {
            // 异常场景也记录审计
            persistAudit(userId, sessionId, userPrompt, "[调用异常] " + e.getMessage(),
                    System.currentTimeMillis() - startTime);
            throw e;
        }

        // 4. 计算耗时 + 提取 AI 响应
        long costTime = System.currentTimeMillis() - startTime;
        String aiResponse = extractAiResponse(result);

        // 5. 持久化审计记录
        persistAudit(userId, sessionId, userPrompt, aiResponse, costTime);

        log.info("审计记录: userId={}, sessionId={}, costTime={}ms", userId, sessionId, costTime);
        return result;
    }

    /**
     * 从 Result<Map<String, String>> 中安全提取 AI 响应
     */
    @SuppressWarnings("unchecked")
    private String extractAiResponse(Object result) {
        try {
            if (result instanceof Result<?> r && r.getData() instanceof Map<?, ?> map) {
                Object response = map.get("response");
                return response != null ? response.toString() : "[响应为空]";
            }
        } catch (Exception e) {
            log.warn("审计响应的提取异常: {}", e.getMessage());
        }
        return "[响应提取失败]";
    }

    /**
     * 持久化审计日志
     */
    private void persistAudit(String userId, String sessionId,
                              String userPrompt, String aiResponse, long costTime) {
        try {
            BiAgentAuditLog log = new BiAgentAuditLog();
            log.setUserId(userId);
            log.setSessionId(sessionId);
            log.setUserPrompt(userPrompt);
            log.setAiResponse(aiResponse);
            log.setCostTime(costTime);
            auditLogMapper.insert(log);
        } catch (Exception e) {
            // 审计失败不应阻断主流程
            log.error("审计日志持久化失败: {}", e.getMessage(), e);
        }
    }
}
