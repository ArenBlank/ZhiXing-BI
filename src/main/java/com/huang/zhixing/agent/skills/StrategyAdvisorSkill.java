package com.huang.zhixing.agent.skills;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Slf4j
@Component
public class StrategyAdvisorSkill {

    @Bean
    public FunctionCallback strategyAdvisor() {
        return FunctionCallback.builder()
                .description("当用户需要商业战略分析、市场定位建议、竞争策略、或行业趋势判断时，调用此技能获取战略分析框架")
                .function("strategyAdvisor", (Function<Request, String>) req -> {
                    log.info("战略咨询技能被调用: topic={}", req.topic());
                    return """
                        你现在以商业战略顾问的身份思考。

                        分析框架：
                        1. 使用波特五力模型分析行业竞争格局。
                        2. SWOT 分析：优势/劣势/机会/威胁，但要具体到可执行的行动。
                        3. 识别关键成功因素——这个行业赢家靠什么。
                        4. 给出差异化定位建议：成本领先、差异化、还是聚焦细分。
                        5. 分析市场时机：为什么现在做（或不做）是对的。
                        6. 提出 3 个可落地的战略举措，按优先级排序。

                        用户需求：%s
                        """.formatted(req.topic());
                })
                .inputType(Request.class)
                .build();
    }

    public record Request(String topic) {}
}
