# ZhiXing-BI

基于 B/S 架构的企业级 AI Agent 平台，支持多格式文件解析、RAG 知识库检索、多轮对话记忆、ReAct 工具链闭环。

## 核心能力

**多格式文件解析引擎** — 基于工厂+模板设计模式，支持 PDF、Word、Excel、PPT、Markdown、TXT 六种格式的自动识别与文本抽取，内置字符集智能探测、大文件流式读取和 OOM 防护。

**RAG 向量知识库** — 文件上传后自动语义切块，通过本地 Ollama（nomic-embed-text）进行 Embedding 向量化，存入 Redis-Stack。检索时使用 RediSearch 的 TAG 索引实现 userId + sessionId 双层物理隔离，确保用户 A 的文档不会出现在用户 B 的搜索结果中。

**多轮对话记忆** — 对话历史全量持久化到 MySQL，每次提问前通过滑动窗口加载最近 N 轮记录并翻转为正序，自动注入 SystemMessage 身份提示和 RAG 知识片段，实现跨会话、跨设备的上下文连续。

**ReAct 工具链** — 基于 Spring AI FunctionCallback 机制，大模型可自主调用以下工具：

| 工具 | 功能 |
|------|------|
| webSearch | DuckDuckGo 全网检索 + 天气查询（wttr.in） |
| stockQuery | A 股实时行情查询（新浪财经 API） |
| getCurrentDateTime | 获取当前系统日期时间 |

**Agent 技能系统** — 从 awesome-llm-apps 开源社区移植的四套商业分析思考框架，大模型根据用户意图自动激活：

| 技能 | 触发场景 |
|------|---------|
| DataAnalyst | 数据分析、趋势判断、量化结论 |
| DecisionHelper | 商业决策、选项比较、利弊分析 |
| StrategyAdvisor | 市场定位、竞争策略、行业趋势 |
| DeepResearch | 深度调研、多源分析、研究报告 |

**联网搜索开关** — 前端输入框上方提供 `○ 联网搜索` 切换按钮，点击后发送的消息自动注入联网指令，大模型优先调用 webSearch 工具获取实时数据；未点击则基于自身知识 + 已上传文件回答。

**文件上传上下文感知** — 上传文件后 AI 自动记录文件名，当用户仅发送"总结""分析"等模糊指令时，自动指向已上传文件；多文件场景下提示用户选择。

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Java 17 / Spring Boot 3.3.5 / Spring AI 1.0.0-M4 / MyBatis-Plus 3.5.9 |
| 前端 | Next.js 16 (App Router) / Tailwind CSS / Anime.js / Lottie-Web / particles.js |
| 数据库 | MySQL 8.0 / Redis-Stack (向量检索 + TAG 索引隔离) |
| AI | DeepSeek (推理) + Ollama nomic-embed-text (本地向量化) |
| 文件解析 | Apache PDFBox 3.0.2 / Apache POI 5.3.0 / Apache Tika |
| 部署 | Vercel (前端) + 内网穿透 (后端) |

## 快速启动

### 1. 启动中间件

```bash
docker compose up -d
```

### 2. 拉取向量模型

```bash
docker exec zhixing-ollama ollama pull nomic-embed-text
```

### 3. 启动后端

```bash
set LLM_API_KEY=你的DeepSeek-Key
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.ai.openai.api-key=%LLM_API_KEY%"
```

### 4. 启动前端

```bash
cd zhixing-bi-front
npm install
npm run dev
```

前端 http://localhost:3000 | 后端 http://localhost:8099

## 项目结构

```
ZhiXing-BI/
├── src/main/java/com/huang/zhixing/
│   ├── agent/
│   │   ├── tools/         # 工具链 (WebSearch, Stock, DateTime)
│   │   └── skills/        # Agent 技能 (数据分析/决策/战略/调研)
│   ├── common/            # Result, AOP审计, JWT, ThreadLocal
│   ├── config/            # Spring AI, Redis, CORS, JWT Filter
│   ├── controller/        # REST 接口 (含 UserController)
│   ├── mapper/            # MyBatis-Plus Mapper
│   ├── model/             # Entity, DTO
│   ├── parser/            # 文件解析引擎 (工厂+模板)
│   └── service/           # 业务逻辑 (记忆, RAG, 会话)
├── zhixing-bi-front/      # Next.js 前端
├── docker-compose.yml
├── pom.xml
├── start.ps1 / stop.ps1   # 一键启停脚本
```
