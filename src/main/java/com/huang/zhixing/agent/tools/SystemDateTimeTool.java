package com.huang.zhixing.agent.tools;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
public class SystemDateTimeTool {

    @Bean
    public FunctionCallback getCurrentDateTime() {
        return FunctionCallback.builder()
                .description("获取当前系统日期和时间，用于商业数据的时间比对和时效性分析")
                .function("getCurrentDateTime", () -> {
                    String now = LocalDateTime.now()
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    log.info("工具调用 - 获取当前时间: {}", now);
                    return now;
                })
                .inputType(Void.class)
                .build();
    }
}
