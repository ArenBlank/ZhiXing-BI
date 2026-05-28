package com.huang.zhixing.agent.skills;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Slf4j
@Component
public class DataAnalystSkill {

    @Bean
    public FunctionCallback dataAnalyst() {
        return FunctionCallback.builder()
                .description("当用户需要对数据进行统计分析、数据洞察、趋势判断、或要求从表格/文件中提取量化结论时，调用此技能获取数据分析方法论指导")
                .function("dataAnalyst", (Function<Request, String>) req -> {
                    log.info("数据分析技能被调用: topic={}", req.topic());
                    return """
                        你现在以资深数据分析师的身份思考。

                        分析框架：
                        1. 先明确数据来源和指标定义，不要上来就算。
                        2. 用描述性统计（均值/中位数/标准差）把握整体分布。
                        3. 发现异常值要追问原因，而不是简单剔除。
                        4. 相关性和因果性要区分清楚——相关性不等于因果。
                        5. 结论要用数据说话，每个论点配一个量化依据。
                        6. 用业务语言解读统计结果，不要堆砌术语。

                        用户需求：%s
                        """.formatted(req.topic());
                })
                .inputType(Request.class)
                .build();
    }

    public record Request(String topic) {}
}
