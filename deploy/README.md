# 部署手册 · 院内内网单机部署

> 目标场景：**无云服务器、无域名、无公网 IP**，手机（维修工/护士长）都在院内 WiFi 下。
> 因此本方案不是"公网云部署"，而是「本机常驻 + 内网直连」——它同样能让手机打开链接，且不把医院数据暴露到公网。

## 一、部署形态（2026-10-17 实测通过）

| 项目 | 值 |
|---|---|
| 后端 | Spring Boot 2.7.18 可执行 jar（`target/desk-0.1.0.jar`，25.28 MB，内置前端静态页） |
| 运行时 | JDK 17（`D:\DevTools\Java\jdk17`）——**系统默认 JAVA_HOME 是 jdk8，必须显式指定** |
| 数据库 | 本机 MySQL 8，库 `it_service_desk`，账号 `itsd` |
| 上传目录 | `<项目根>/uploads`（可用 `FILE_UPLOAD_DIR` 覆盖） |
| 日志 | `deploy/logs/desk-<时间戳>.out.log` / `.err.log` |
| 监听 | `0.0.0.0:8080`（所有网卡，手机才连得进来） |
| 访问地址 | `http://10.31.67.107:8080` |

前端、后端、静态资源全部打在同一个 jar 里，**没有独立前端服务**，也没有 Nginx 需要维护。

## 二、启动 / 停止

```powershell
# 启动（默认 8080，自动探测内网 IP 作为通知链接地址）
powershell -NoProfile -ExecutionPolicy Bypass -File deploy\start-desk.ps1

# 指定端口 / 指定对外地址
powershell -NoProfile -ExecutionPolicy Bypass -File deploy\start-desk.ps1 -Port 8080 -PublicHost http://10.31.67.107:8080

# 停止
powershell -NoProfile -ExecutionPolicy Bypass -File deploy\stop-desk.ps1
```

`start-desk.ps1` 会自动做四件事：端口占用检查 → 内网 IP 探测（排除 Hyper-V/VMware/WSL 虚拟网卡）→ JDK17 校验 → 启动并**轮询到 200 才算成功**（失败会打印日志尾部）。

打包：

```powershell
$env:JAVA_HOME="D:\DevTools\Java\jdk17"
mvn clean package "-DskipTests" "-Dmaven.repo.local=.m2repo"
```

> 打包前必须先停掉占用 `target/desk-0.1.0.jar` 的 java 进程，否则报 `Unable to rename ... .jar.original`。

## 三、运行期环境变量（全部可覆盖，不必改代码）

| 变量 | 作用 | 默认 |
|---|---|---|
| `SERVER_PORT` | 监听端口 | `8080` |
| `NOTIFY_PUBLIC_HOST` | **企业微信通知里链接的地址**，必须是手机能打开的地址 | 空（= 功能关闭） |
| `FILE_UPLOAD_DIR` | 签名/照片落盘目录 | `D:/.../uploads` |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | 数据库连接 | `localhost:3306/it_service_desk` / `itsd` / 见 `application.yml` |

**`NOTIFY_PUBLIC_HOST` 是"通知链接打不开"的根因**：留空时企微消息里没有可点地址；必须填成 `http://<内网IP>:<端口>`。启动脚本已自动探测填入。

## 四、手机验收步骤（可验证）

1. 手机连**院内 WiFi**（需与 `10.31.67.107` 同网段，掩码 `255.255.0.0`）。
2. 扫 `deploy/qr/report.png`（科室报修端）、`deploy/qr/today.png`（维修端）、`deploy/qr/login.png`（后台）。
   > **二维码与端口绑定**：由 `/api/qr` 按请求 Host 动态生成，当前三张指向 **8081**。统一到 8080 后需重新生成（登录后台 →「系统信息」页重新出码，或重跑生成脚本），否则扫码会落到旧端口。
3. 期望：报修页正常渲染出三级联动（院区→楼栋→科室），提交后后台「待受理报修单」立即可见。
4. 若打不开 → 90% 是防火墙，见下一节。

## 五、防火墙放行（**必须管理员权限**，一次即可）

```powershell
# 管理员 PowerShell 里执行
netsh advfirewall firewall add rule name="ITSD-8080" dir=in action=allow protocol=TCP localport=8080
# 校验
netsh advfirewall firewall show rule name="ITSD-8080"
```

> 本机访问 `http://10.31.67.107:8080` 成功 **不等于** 手机能访问：本机访问走的是回环路径，不经过入站防火墙规则。**手机实测才是唯一有效验收。**

## 六、开机自启（可选）

```powershell
schtasks /create /tn "ITSD-Deploy" /sc onlogon ^
  /tr "powershell -NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -File D:\DeepSeek_data\gradaute_space\it-service-desk\deploy\start-desk.ps1"
```

> 注意：`onlogon` 需要该 Windows 账号**登录**才会触发；无人值守需用「服务化」（推荐 NSSM / WinSW），本机未安装。

## 七、本次部署的已知限制

1. **HTTP 下 PWA 不可用**：`navigator.serviceWorker` 只在 HTTPS 或 localhost 存在。手机走 `http://10.31.67.107:8080` 时 SW 注册被页面守卫跳过，**「添加到主屏」不可用**，页面功能不受影响。要 PWA 必须上 HTTPS（自签证书手机仍不信任 → 需真域名或内网 CA）。
2. **无 HTTPS / 无域名**：企微通知里的链接只能是内网 http 地址，离开院内 WiFi 必然打不开。
3. **单机无守护**：进程崩溃不会自动重启（除非配计划任务/服务化）。
4. **数据库口令仍是默认值**（`application.yml`），仅因限定在内网访问才可接受；上公网前必须改。
5. **内网 IP 可能变**：`10.31.67.107` 若由 DHCP 分配，重连后地址会变，二维码/通知链接随之失效 → 建议找网管做 DHCP 保留或改静态。
6. **依赖本机常开**：电脑关机/休眠 = 服务不可用。

## 八、将来要"真上云"还差什么

| 缺口 | 说明 |
|---|---|
| 云服务器 | 2C4G 起，Linux，装 JDK17 或直接用 Docker |
| 域名 + HTTPS | 国内需备案；企微通知链接要能点必须是公网可达 + 可信证书 |
| 数据库 | 云 RDS MySQL 8，或容器内 MySQL + 卷挂载 |
| 数据合规 | 含科室/维修/报销数据，公网暴露需评估医院信息安全要求——**内网部署反而是更稳妥的选择** |

技术上无需改代码：jar 已把配置全部外置为环境变量，`DB_URL`/`FILE_UPLOAD_DIR`/`NOTIFY_PUBLIC_HOST` 三个变量换掉即可迁移；`schema.sql`/`data.sql` 为幂等语句（`CREATE TABLE IF NOT EXISTS` + `INSERT IGNORE`），空库首启会自动建表并灌入科室 68 条与演示账号 6 个。
