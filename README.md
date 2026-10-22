# The Last · 随处记录

[下载最新版](https://github.com/9OwO6/The-Last/releases/latest) · [更新与发布说明](docs/updates.md)

本地优先的 Android 事件记录器。可以一键记录、补记、编辑历史、查看频率、导出 JSON/CSV，并支持 MyJournal 私人云同步和网页记录入口。

0.12.0「小熊生活手账」加入统一 Q 版绘本角色：首页收纳生活、日历翻看回忆、统计照料小花、更新递送包裹、空状态等待动笔。配合纸色卡片、中文日历和清晰的功能导航。插画用途及完整生成提示词见 [视觉设计说明](docs/art-direction-0.12.md)。

0.11.0 优化暖纸色界面、首页生活插画与事项图标，新增「我的 → 软件更新」：启动/返回前台自动检查正式 GitHub Release，发现新版在首页提醒；支持更新说明、下载进度、SHA-256 与签名校验，以及系统确认安装。完整发布与验收约定见 [更新说明](docs/updates.md)。

## 跨设备使用

1. 更新软件前，先在「我的 → 导出备份」保存 JSON。覆盖安装会保留 Room 数据；数据库升级为旧记录分配稳定的云端 UUID。
2. 在「我的 → 云端同步」连接 `https://my-journal-amber.vercel.app`。
3. 发起连接后，点击「浏览器授权」，登录 MyJournal，核对 10 位连接码并确认，再回到软件检查连接。
4. 手机 B 无需安装软件，打开 [手机快捷页](https://my-journal-amber.vercel.app/mobile/thelast)，登录后即可一键记录。可把网页添加到主屏幕。
5. MyJournal [The Last 完整页面](https://my-journal-amber.vercel.app/thelast) 提供历史、补记、编辑、归档、频率和设备撤销。

两端共用事项和历史；同名事项不会自动合并。首次连接会上传已有本机记录。

## 同步行为

- 本机写入与同步队列在同一个 Room 事务提交；离线也能记录。
- 启动、回到前台、记录之后和网络恢复时尝试同步。软件完全退出后，下次打开继续。
- 重试使用持久化操作 UUID，避免重复；删除标记防止旧设备让记录复活。
- 两端同时修改同一条数据时保留本机草稿；先导出「完整同步草稿」，再明确选择云端版本。
- 设备凭证由 Android Keystore 加密，不参与系统备份。软件没有 Supabase 高权限密钥。
- 网页可撤销设备云端权限；「断开本机」保留本地记录并停止本机同步。
- JSON/CSV 备份仍可独立使用。手动重复导入旧备份会创建新事项，和自动同步重试不同。

## 开发与验证

Kotlin / Compose / Room，最低 Android 8（API 26），JDK 17。

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

普通测试覆盖旧库升级、UUID 重启稳定、离线汇合、重复上传、上传途中修改、冲突与删除。可选 `CloudLiveTest` 使用 `THELAST_QA_CONFIG` 指向临时测试设备配置，验证实际 HTTP → API → Supabase 链路；未提供配置时跳过这一项。测试配置和凭证不得提交。

网页与 SQL 源码位于 `E:\Code\MyJournal\myjournal`，设计和部署边界见该项目 `docs/thelast-cloud.md`。
