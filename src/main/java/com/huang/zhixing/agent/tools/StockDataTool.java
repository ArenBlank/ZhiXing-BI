package com.huang.zhixing.agent.tools;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.function.Function;

@Slf4j
@Component
public class StockDataTool {

    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8)).build();

    @Bean
    public FunctionCallback stockQuery() {
        return FunctionCallback.builder()
                .description("查询A股实时行情数据。传入股票代码（如sh600519贵州茅台、sz000001平安银行）或指数代码（如sh000001上证指数、sz399001深证成指），返回最新价、涨跌幅、成交量等实时数据")
                .function("stockQuery", (Function<StockRequest, String>) req -> {
                    log.info("股票查询: code={}", req.code());
                    try {
                        return fetchStock(req.code());
                    } catch (Exception e) {
                        log.error("股票查询异常: {}", e.getMessage());
                        return "股票数据获取失败: " + e.getMessage();
                    }
                })
                .inputType(StockRequest.class)
                .build();
    }

    private String fetchStock(String code) throws Exception {
        // 新浪财经实时行情接口 — 免费无需 Key
        String url = "https://hq.sinajs.cn/list=" + code;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(8))
                .header("Referer", "https://finance.sina.com.cn")
                .GET().build();

        HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString(Charset.forName("GBK")));
        String body = resp.body();
        if (body == null || body.isBlank() || body.contains("\"\"")) {
            return "未找到股票代码 " + code + " 的实时数据，请检查代码是否正确";
        }

        // 解析新浪返回格式
        String data = body.split("\"")[1];
        String[] f = data.split(",");
        if (f.length < 32) return "数据格式异常: " + data;

        String name = f[0];
        String open = f[1], prev = f[2], price = f[3], high = f[4], low = f[5];
        String volume = f[8], amount = f[9];
        return String.format("""
                【%s(%s) 实时行情】
                最新价: %s | 今开: %s | 昨收: %s
                最高: %s | 最低: %s
                成交量: %s手 | 成交额: %s万元""",
                name, code, price, open, prev, high, low, volume, amount);
    }

    public record StockRequest(String code) {}
}
