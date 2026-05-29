chcp 65001 > $null
$root = "D:\DevelopmentLOOK\Idea\idea_project_workspace\ZhiXing-BI"
Set-Location $root

Write-Host ">>> Stopping Java backend..." -ForegroundColor Cyan
Get-CimInstance Win32_Process |
  Where-Object { $_.ProcessId -ne $PID -and $_.Name -eq "java.exe" } |
  ForEach-Object {
    Write-Host "  PID $($_.ProcessId)" -ForegroundColor Gray
    Stop-Process -Id $_.ProcessId -Force
  }

Write-Host ">>> Stopping Node.js frontend..." -ForegroundColor Cyan
Get-CimInstance Win32_Process |
  Where-Object { $_.ProcessId -ne $PID -and $_.Name -eq "node.exe" } |
  ForEach-Object {
    Write-Host "  PID $($_.ProcessId)" -ForegroundColor Gray
    Stop-Process -Id $_.ProcessId -Force
  }

Write-Host ">>> Stopping Docker containers..." -ForegroundColor Cyan
docker compose -f docker-compose.yml stop mysql redis ollama

Write-Host ""
Write-Host "All services stopped." -ForegroundColor Green
