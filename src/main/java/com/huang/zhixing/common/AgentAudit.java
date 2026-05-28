package com.huang.zhixing.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 企业级 Agent 审计注解 —— 非侵入式挂载到 Controller 方法上，
 * 由 AgentAuditAspect 切面自动拦截并持久化用户行为、Prompt、AI 响应与耗时。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AgentAudit {
}
