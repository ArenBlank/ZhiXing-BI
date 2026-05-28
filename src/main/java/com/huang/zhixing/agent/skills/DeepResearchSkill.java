package com.huang.zhixing.agent.skills;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Slf4j
@Component
public class DeepResearchSkill {

    @Bean
    public FunctionCallback deepResearch() {
        return FunctionCallback.builder()
                .description("当用户需要进行深度调研、多源信息综合分析、或撰写研究报告时，调用此技能获取深度研究方法论")
                .function("deepResearch", (Function<Request, String>) req -> {
                    log.info("深度调研技能被调用: topic={}", req.topic());
                    return """
                        你现在以深度研究分析师的身份思考。

                        研究框架：
                        1. 先定义研究范围——这个主题的边界在哪里，哪些相关但本次不深入。
                        2. 从多个角度切入：历史演进、当前现状、未来趋势。
                        3. 每个关键结论标注信息来源和置信度。
                        4. 区分事实、观点和推测，不要让后两者伪装成前者。
                        5. 找出不同来源之间的共识和分歧点。
                        6. 提出 3 个值得进一步追踪的子课题。
                        7. 用"首先…其次…最后"组织段落，确保逻辑链条清晰。

                        用户需求：%s
                        """.formatted(req.topic());
                })
                .inputType(Request.class)
                .build();
    }

    public record Request(String topic) {}
}
