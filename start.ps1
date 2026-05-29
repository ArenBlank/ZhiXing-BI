chcp 65001 > $null
$ErrorActionPreference = "Stop"
$root = "D:\DevelopmentLOOK\Idea\idea_project_workspace\ZhiXing-BI"
Set-Location $root

if (-not $env:LLM_API_KEY) {
    Write-Host "[ERROR] LLM_API_KEY not set!" -ForegroundColor Red
    Write-Host "  Run: `$env:LLM_API_KEY = 'your-deepseek-key'"
    exit 1
}

Write-Host ""
Write-Host ">>> Starting Docker services..." -ForegroundColor Cyan
docker compose -f docker-compose.yml up -d mysql redis ollama

$models = docker exec zhixing-ollama ollama list 2>&1
if ($models -notmatch "nomic-embed-text") {
    Write-Host "  Pulling nomic-embed-text model..." -ForegroundColor Yellow
    docker exec zhixing-ollama ollama pull nomic-embed-text
}

Write-Host ""
Write-Host ">>> Stopping old Java processes..." -ForegroundColor Cyan
Get-CimInstance Win32_Process |
  Where-Object { $_.ProcessId -ne $PID -and $_.Name -eq "java.exe" } |
  ForEach-Object { Stop-Process -Id $_.ProcessId -Force }

Write-Host ""
Write-Host ">>> Starting backend on port 8099..." -ForegroundColor Cyan
Write-Host "    Wait for 'Started ZhiXingApplication'"
Write-Host ""

$mvnOpts = '-Dspring-boot.run.arguments=--spring.ai.openai.api-key=' + $env:LLM_API_KEY
mvn spring-boot:run $mvnOpts
