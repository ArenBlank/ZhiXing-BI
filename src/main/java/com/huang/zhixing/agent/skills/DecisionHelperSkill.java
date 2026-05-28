package com.huang.zhixing.agent.skills;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Slf4j
@Component
public class DecisionHelperSkill {

    @Bean
    public FunctionCallback decisionHelper() {
        return FunctionCallback.builder()
                .description("当用户面临商业决策、需要在多个选项中做选择、或要求进行利弊分析时，调用此技能获取结构化决策框架")
                .function("decisionHelper", (Function<Request, String>) req -> {
                    log.info("决策辅助技能被调用: topic={}", req.topic());
                    return """
                        你现在以商业决策顾问的身份思考。

                        决策框架：
                        1. 先拆解决策维度：成本、收益、风险、时间、资源。
                        2. 每个选项列出 3 个最大优势和 3 个最大劣势。
                        3. 区分"必须满足的条件"和"锦上添花的加分项"。
                        4. 考虑不做这个决策的后果——零选项分析。
                        5. 给出推荐选项，并说明推荐理由和隐含假设。
                        6. 如果信息不足，明确指出还需要什么数据才能做更优判断。

                        用户需求：%s
                        """.formatted(req.topic());
                })
                .inputType(Request.class)
                .build();
    }

    public record Request(String topic) {}
}
