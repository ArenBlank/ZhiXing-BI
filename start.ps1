# ZhiXing-BI 一键启动脚本
# 用法：右键此文件 → 使用 PowerShell 运行
# 或：PowerShell 中执行 .\start.ps1

$ErrorActionPreference = "Stop"
$root = "D:\DevelopmentLOOK\Idea\idea_project_workspace\ZhiXing-BI"
Set-Location $root

# ============================================================
# 检查 LLM_API_KEY
# ============================================================
if (-not $env:LLM_API_KEY) {
    Write-Host "[错误] 未设置环境变量 LLM_API_KEY" -ForegroundColor Red
    Write-Host "  临时设置: `$env:LLM_API_KEY = `"你的DeepSeek密钥`""
    Write-Host "  永久设置: Windows 系统属性 → 环境变量 → 新建 LLM_API_KEY"
    exit 1
}

# ============================================================
# 1. 启动 Docker 中间件
# ============================================================
Write-Host ""
Write-Host ">>> 启动 Docker 中间件..." -ForegroundColor Cyan
docker compose -f docker-compose.yml up -d mysql redis ollama

Write-Host "  MySQL      → 127.0.0.1:3309 (root/root, 库: zhixing_bi_db)" -ForegroundColor Green
Write-Host "  RedisStack → 127.0.0.1:6389" -ForegroundColor Green
Write-Host "  Ollama     → 127.0.0.1:11434" -ForegroundColor Green

# 首次运行需要拉取向量模型
$models = docker exec zhixing-ollama ollama list 2>&1
if ($models -notmatch "nomic-embed-text") {
    Write-Host "  首次运行，拉取 nomic-embed-text 向量模型..." -ForegroundColor Yellow
    docker exec zhixing-ollama ollama pull nomic-embed-text
}

# ============================================================
# 2. 停止旧 Java 进程
# ============================================================
Write-Host ""
Write-Host ">>> 停止旧的 Java 进程..." -ForegroundColor Cyan
Get-CimInstance Win32_Process |
  Where-Object { $_.ProcessId -ne $PID -and $_.Name -eq "java.exe" } |
  ForEach-Object {
    Write-Host "  停止 PID $($_.ProcessId)" -ForegroundColor Gray
    Stop-Process -Id $_.ProcessId -Force
  }

# ============================================================
# 3. 启动后端
# ============================================================
Write-Host ""
Write-Host ">>> 启动后端 (端口 8099)..." -ForegroundColor Cyan
Write-Host "  看到 Started ZhiXingApplication 即启动成功"
Write-Host "  日志实时输出，Ctrl+C 停止"
Write-Host ""

mvn spring-boot:run "-Dspring-boot.run.arguments=--spring.ai.openai.api-key=$env:LLM_API_KEY"
