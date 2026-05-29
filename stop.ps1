# ZhiXing-BI 一键关闭脚本
# 用法：右键此文件 → 使用 PowerShell 运行

$root = "D:\DevelopmentLOOK\Idea\idea_project_workspace\ZhiXing-BI"
Set-Location $root

# ============================================================
# 1. 关闭 Java 后端
# ============================================================
Write-Host ">>> 关闭 Java 后端..." -ForegroundColor Cyan
Get-CimInstance Win32_Process |
  Where-Object { $_.ProcessId -ne $PID -and $_.Name -eq "java.exe" } |
  ForEach-Object {
    Write-Host "  停止 PID $($_.ProcessId)" -ForegroundColor Gray
    Stop-Process -Id $_.ProcessId -Force
  }

# ============================================================
# 2. 关闭 Node.js 前端
# ============================================================
Write-Host ">>> 关闭 Node.js 前端..." -ForegroundColor Cyan
Get-CimInstance Win32_Process |
  Where-Object { $_.ProcessId -ne $PID -and $_.Name -eq "node.exe" } |
  ForEach-Object {
    Write-Host "  停止 PID $($_.ProcessId)" -ForegroundColor Gray
    Stop-Process -Id $_.ProcessId -Force
  }

# ============================================================
# 3. 关闭 Docker 中间件（仅本项目）
# ============================================================
Write-Host ">>> 关闭 Docker 中间件..." -ForegroundColor Cyan
docker compose -f docker-compose.yml stop mysql redis ollama

Write-Host ""
Write-Host "所有服务已关闭" -ForegroundColor Green
