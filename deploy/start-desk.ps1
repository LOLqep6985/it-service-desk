<#
  启动「多院区 IT 运维工单系统」（院内内网常驻部署）
  用法：
    powershell -ExecutionPolicy Bypass -File deploy\start-desk.ps1
    powershell -ExecutionPolicy Bypass -File deploy\start-desk.ps1 -Port 8080

  说明：
    - 监听 0.0.0.0，同一 WiFi 下的手机可用 http://<本机内网IP>:<Port> 访问（扫码/输入地址即可）
    - 桌面图标由使用人员自行用手机浏览器菜单「添加到主屏幕」创建，不做安装引导
    - 数据库口令不写在本文件里：沿用 application.yml 默认值，或用环境变量 DB_URL/DB_USERNAME/DB_PASSWORD 覆盖
#>
param(
  [int]$Port = 8080
)

$ErrorActionPreference = "Stop"

$AppRoot = Split-Path -Parent $PSScriptRoot
$Jar = Join-Path $AppRoot "target\desk-0.1.0.jar"
if (-not (Test-Path $Jar)) {
  throw "未找到 jar：$Jar`n请先打包： cd it-service-desk; mvn clean package -DskipTests -Dmaven.repo.local=.m2repo"
}

# ---- 1. 端口占用检查（避免重复启动/静默失败）----
# 注意：不用 Get-NetTCPConnection——它依赖 CIM，在受限环境会静默返回空，导致误判端口空闲
$busyPids = @()
foreach ($line in (netstat -ano 2>$null)) {
  if ("$line" -match ":$Port\s" -and "$line" -match 'LISTENING\s+(\d+)\s*$') { $busyPids += [int]$Matches[1] }
}
$busyPids = $busyPids | Sort-Object -Unique
if ($busyPids) {
  Write-Host "[ABORT] 端口 $Port 已被占用（PID: $($busyPids -join ',')）" -ForegroundColor Red
  Write-Host "        若确认是本系统旧进程： powershell -File deploy\stop-desk.ps1 -Port $Port" -ForegroundColor Red
  exit 1
}

# ---- 2. 探测本机内网 IPv4（避开 127.x / 169.254.x）----
function Get-LanIPv4 {
  # 只认物理/无线网卡，排除 Hyper-V、VMware、WSL 等虚拟适配器
  # （否则会误取 172.23.240.1 这类虚拟网段，手机根本访问不到）
  $exclude = 'Virtual|VMware|Hyper-V|VBox|VirtualBox|Loopback|TAP-|WAN Miniport|Pseudo|Tailscale|ZeroTier|Bluetooth'
  try {
    $nics = [System.Net.NetworkInformation.NetworkInterface]::GetAllNetworkInterfaces() |
      Where-Object {
        $_.OperationalStatus -eq 'Up' -and
        ($_.NetworkInterfaceType -eq 'Wireless80211' -or $_.NetworkInterfaceType -eq 'Ethernet') -and
        $_.Description -notmatch $exclude
      }
    $ips = foreach ($n in $nics) {
      $a = $n.GetIPProperties().UnicastAddresses |
        Where-Object { $_.Address.AddressFamily -eq 'InterNetwork' } |
        Select-Object -First 1
      if ($a) { [pscustomobject]@{ IP = $a.Address.IPAddressToString; Type = "$($n.NetworkInterfaceType)" } }
    }
    $ips = $ips | Where-Object { $_ -and $_.IP -notlike '169.254.*' -and $_.IP -notlike '127.*' }
    $wifi = $ips | Where-Object { $_.Type -eq 'Wireless80211' }
    if ($wifi) { return @($wifi)[0].IP }
    if ($ips) { return @($ips)[0].IP }
  } catch { }
  return $null
}

# ---- 2b. 打印手机访问地址（仅提示用，不写入任何配置）----
$lanIp = Get-LanIPv4
if ($lanIp) {
  $phoneUrl = "http://${lanIp}:$Port"
  Write-Host "[INFO] 手机访问地址：$phoneUrl"
} else {
  $phoneUrl = "(未探测到内网 IPv4)"
  Write-Host "[WARN] 未探测到内网 IPv4，手机请用本机实际内网地址访问" -ForegroundColor Yellow
}

# ---- 3. JDK：必须 17+（本机默认 JAVA_HOME 是 jdk8，会报 Class Version 61）----
function Get-JavaMajor([string]$exe) {
  # java -version 输出到 stderr；脚本里 ErrorActionPreference=Stop 会把它当异常，这里临时放开
  $prev = $ErrorActionPreference
  $ErrorActionPreference = 'Continue'
  $text = ""
  try { $text = (& $exe -version 2>&1 | Out-String) } catch { $text = "$_" }
  finally { $ErrorActionPreference = $prev }
  if ($text -match 'version "(\d+)') {
    $v = [int]$Matches[1]
    if ($v -eq 1 -and $text -match 'version "1\.(\d+)') { $v = [int]$Matches[1] }  # 1.8.0 -> 8
    return $v
  }
  return 0
}

$javaCandidates = @()
if ($env:JDK17_HOME) { $javaCandidates += (Join-Path $env:JDK17_HOME "bin\java.exe") }
$javaCandidates += "D:\DevTools\Java\jdk17\bin\java.exe"
if ($env:JAVA_HOME) { $javaCandidates += (Join-Path $env:JAVA_HOME "bin\java.exe") }
$javaCandidates += "java.exe"

$java = $null
foreach ($c in $javaCandidates) {
  if (-not $c) { continue }
  if ($c -ne "java.exe" -and -not (Test-Path $c)) { continue }
  try {
    $maj = Get-JavaMajor $c
    if ($maj -ge 17) { $java = $c; break }
    Write-Host "[SKIP] $c 是 Java $maj，需要 17+"
  } catch { }
}
if (-not $java) { throw "未找到 Java 17+。请安装 JDK17，或设置环境变量 JDK17_HOME 指向 JDK17 安装目录。" }
# 让子进程沿用同一个 JDK（jar 是绝对路径时才能推导出 JDK 根目录）
if ($java -ne "java.exe") { $env:JAVA_HOME = Split-Path -Parent (Split-Path -Parent $java) }
Write-Host "[INFO] 使用 Java：$java"

# ---- 4. 运行期环境变量 ----
$env:FILE_UPLOAD_DIR = Join-Path $AppRoot "uploads"
$env:SERVER_PORT = "$Port"
# 首次启动由后端自动建表 + 灌基础数据（schema.sql/data.sql 均为幂等语句）
if (-not $env:SQL_INIT_MODE) { $env:SQL_INIT_MODE = "always" }

New-Item -ItemType Directory -Force -Path (Join-Path $AppRoot "uploads") | Out-Null
$LogDir = Join-Path $PSScriptRoot "logs"
New-Item -ItemType Directory -Force -Path $LogDir | Out-Null
$stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$outLog = Join-Path $LogDir "desk-$stamp.out.log"
$errLog = Join-Path $LogDir "desk-$stamp.err.log"

# ---- 5. 启动 ----
$proc = Start-Process -FilePath $java `
  -ArgumentList @("-jar", "`"$Jar`"", "-Dfile.encoding=UTF-8") `
  -WorkingDirectory $AppRoot `
  -WindowStyle Hidden `
  -RedirectStandardOutput $outLog `
  -RedirectStandardError $errLog `
  -PassThru

Write-Host "[OK] 已启动 PID=$($proc.Id)"
Write-Host "     端口    : $Port"
Write-Host "     手机访问: $phoneUrl"
Write-Host "     日志    : $outLog"
Write-Host "     停止    : powershell -ExecutionPolicy Bypass -File deploy\stop-desk.ps1"

# ---- 6. 等待就绪（最多 60 秒）----
$deadline = (Get-Date).AddSeconds(60)
$ok = $false
while ((Get-Date) -lt $deadline) {
  Start-Sleep -Seconds 2
  if ($proc.HasExited) {
    Write-Host "[FAIL] 进程已退出（exit=$($proc.ExitCode)），日志尾部：" -ForegroundColor Red
    Get-Content $outLog -Tail 30 -ErrorAction SilentlyContinue
    Get-Content $errLog -Tail 30 -ErrorAction SilentlyContinue
    exit 1
  }
  try {
    $r = Invoke-WebRequest -Uri "http://127.0.0.1:$Port/login.html" -UseBasicParsing -TimeoutSec 3
    if ($r.StatusCode -eq 200) { $ok = $true; break }
  } catch { }
}
if ($ok) {
  Write-Host "[READY] http://127.0.0.1:$Port/login.html 返回 200" -ForegroundColor Green
  exit 0
} else {
  Write-Host "[WARN] 60 秒内未就绪，请查看日志：$outLog" -ForegroundColor Yellow
  exit 2
}
