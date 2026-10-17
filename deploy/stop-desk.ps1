<#
  停止「多院区 IT 运维工单系统」
  用法：
    powershell -ExecutionPolicy Bypass -File deploy\stop-desk.ps1              # 按 jar 命令行找
    powershell -ExecutionPolicy Bypass -File deploy\stop-desk.ps1 -Port 8081   # 按端口找（兜底）

  说明：
    - 优先按命令行匹配 desk-0.1.0.jar（不会命中 IDEA 里以 classpath 启动的开发实例）
    - CIM 不可用时（受限环境）退化为「按端口找 PID」，且**只杀 java.exe**，避免误伤别的程序
#>
param(
  [int]$Port = 0
)

$ErrorActionPreference = "Continue"
$jarName = "desk-0.1.0.jar"

function Get-PidsOnPort([int]$p) {
  $found = @()
  foreach ($line in (netstat -ano 2>$null)) {
    if ("$line" -match ":$p\s" -and "$line" -match 'LISTENING\s+(\d+)\s*$') {
      $found += [int]$Matches[1]
    }
  }
  return ($found | Sort-Object -Unique)
}

# ---- 1. 优先：按 jar 命令行匹配 ----
$targets = @()
$procs = Get-CimInstance Win32_Process -Filter "Name = 'java.exe'" -ErrorAction SilentlyContinue
if ($procs) {
  $targets = @($procs | Where-Object { $_.CommandLine -and $_.CommandLine -like "*$jarName*" } |
    Select-Object -ExpandProperty ProcessId)
}

# ---- 2. 兜底：按端口找，且必须是 java ----
if (-not $targets -and $Port -gt 0) {
  foreach ($p in (Get-PidsOnPort $Port)) {
    $pn = $null
    try { $pn = (Get-Process -Id $p -ErrorAction Stop).ProcessName } catch { $pn = $null }
    if ($pn -eq 'java') { $targets += $p }
    elseif ($pn) { Write-Host "[SKIP] 端口 $Port 的 PID=$p 是 $pn，不是 java，不动它" -ForegroundColor Yellow }
    else { Write-Host "[WARN] 端口 $Port 的 PID=$p 看不到进程详情，无法确认是 java，已跳过（可在任务管理器手动结束）" -ForegroundColor Yellow }
  }
}

if (-not $targets) {
  Write-Host "[INFO] 没有发现运行中的 $jarName"
  exit 0
}

foreach ($p in ($targets | Sort-Object -Unique)) {
  Write-Host "停止 PID=$p"
  Stop-Process -Id $p -Force -ErrorAction SilentlyContinue
}

Start-Sleep -Seconds 2

# ---- 3. 校验 ----
$left = @()
foreach ($p in ($targets | Sort-Object -Unique)) {
  if (Get-Process -Id $p -ErrorAction SilentlyContinue) { $left += $p }
}
if ($left) {
  Write-Host "[WARN] 仍有进程存活：$($left -join ',')" -ForegroundColor Yellow
  exit 1
}
if ($Port -gt 0) {
  $still = Get-PidsOnPort $Port
  if ($still) {
    Write-Host "[WARN] 端口 $Port 仍被占用（PID: $($still -join ',')）" -ForegroundColor Yellow
    exit 1
  }
  Write-Host "[OK] 已停止，端口 $Port 已释放" -ForegroundColor Green
} else {
  Write-Host "[OK] 已停止" -ForegroundColor Green
}
exit 0
