package com.huang.zhixing.agent.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.Function;

@Slf4j
@Component
public class WebSearchTool {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    @Bean
    public FunctionCallback webSearch() {
        return FunctionCallback.builder()
                .description("搜索互联网获取最新信息。当本地知识库无法提供最新信息时，调用此工具传入关键词进行全网检索")
                .function("webSearch", (Function<SearchRequest, String>) req -> {
                    log.info("联网搜索: keyword={}", req.keyword());
                    try {
                        return realSearch(req.keyword());
                    } catch (Exception e) {
                        log.error("搜索异常: {}", e.getMessage());
                        return fallbackSearch(req.keyword());
                    }
                })
                .inputType(SearchRequest.class)
                .build();
    }

    /**
     * DuckDuckGo Instant Answer API — 免费、无需 Key
     */
    /**
     * 检测是否为天气类查询 → 走 wttr.in（免费无需 Key）
     */
    private boolean isWeatherQuery(String keyword) {
        String kw = keyword.toLowerCase();
        return kw.contains("天气") || kw.contains("气温") || kw.contains("温度") || kw.contains("weather")
            || kw.contains("下雨") || kw.contains("晴天") || kw.contains("台风") || kw.contains("降水");
    }

    private String realSearch(String keyword) throws Exception {
        // 天气查询走 wttr.in
        if (isWeatherQuery(keyword)) {
            return searchWeather(keyword);
        }

        String encoded = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
        String url = "https://api.duckduckgo.com/?q=" + encoded + "&format=json&no_html=1&skip_disambig=1";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("User-Agent", "ZhiXing-BI/1.0")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode root = mapper.readTree(response.body());

        StringBuilder sb = new StringBuilder();
        sb.append("【DuckDuckGo 搜索结果 - ").append(keyword).append("】\n\n");

        // 摘要
        String abstractText = root.path("AbstractText").asText("");
        if (!abstractText.isBlank()) {
            sb.append(abstractText).append("\n\n");
            String source = root.path("AbstractURL").asText("");
            if (!source.isBlank()) sb.append("来源: ").append(source).append("\n\n");
        }

        // 相关话题
        JsonNode topics = root.path("RelatedTopics");
        int count = 0;
        for (JsonNode topic : topics) {
            if (count >= 5) break;
            String text = topic.path("Text").asText("");
            if (!text.isBlank()) {
                sb.append(count + 1).append(". ").append(text).append("\n");
                count++;
            }
        }

        if (sb.toString().equals("【DuckDuckGo 搜索结果 - " + keyword + "】\n\n")) {
            return fallbackSearch(keyword);
        }

        return sb.toString().trim();
    }

    /**
     * wttr.in 天气查询 —— 免费，无需 API Key
     */
    private String searchWeather(String keyword) throws Exception {
        // 从关键词中提取城市名，或用默认值
        String city = keyword.replaceAll("天气|气温|温度|今天|明天|预报|查询|最新|实时|几度|多少度|怎么样", "");
        city = city.trim().isEmpty() ? "guangzhou" : city.trim();

        String url = "https://wttr.in/" + URLEncoder.encode(city, StandardCharsets.UTF_8)
                + "?format=j1&lang=zh";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("User-Agent", "curl/8.0")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode root = mapper.readTree(response.body());

        StringBuilder sb = new StringBuilder();
        sb.append("【实时天气 - wttr.in】\n");

        JsonNode current = root.path("current_condition").get(0);
        if (!current.isMissingNode()) {
            sb.append("温度: ").append(current.path("temp_C").asText()).append("°C\n");
            sb.append("体感: ").append(current.path("FeelsLikeC").asText()).append("°C\n");
            sb.append("天气: ").append(current.path("weatherDesc").get(0).path("value").asText()).append("\n");
            sb.append("湿度: ").append(current.path("humidity").asText()).append("%\n");
            sb.append("风速: ").append(current.path("windspeedKmph").asText()).append("km/h\n");
            sb.append("风向: ").append(current.path("winddir16Point").asText()).append("\n");
        }

        // 未来几天预报
        JsonNode forecast = root.path("weather");
        if (forecast.isArray() && forecast.size() > 0) {
            sb.append("\n未来预报:\n");
            for (int i = 0; i < Math.min(3, forecast.size()); i++) {
                JsonNode day = forecast.get(i);
                sb.append(day.path("date").asText()).append(": ");
                sb.append("最高").append(day.path("maxtempC").asText()).append("°C ");
                sb.append("最低").append(day.path("mintempC").asText()).append("°C ");
                JsonNode hourly = day.path("hourly");
                if (hourly.isArray() && hourly.size() > 0) {
                    sb.append(hourly.get(0).path("weatherDesc").get(0).path("value").asText());
                }
                sb.append("\n");
            }
        }

        return sb.toString().trim();
    }

    /**
     * DuckDuckGo 无结果时的 HTML 抓取降级
     */
    private String fallbackSearch(String keyword) {
        return "【联网搜索 - " + keyword + "】\n" +
               "DuckDuckGo 未返回直接结果。建议你基于已有知识和训练数据进行分析，" +
               "或在搜索引擎手动搜索: https://duckduckgo.com/?q=" +
               URLEncoder.encode(keyword, StandardCharsets.UTF_8);
    }

    public record SearchRequest(String keyword) {}
}
