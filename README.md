# StreamBox TV

一个面向 Android TV / 电视盒子的自用 IPTV 播放器原型。

## 已实现

- 内置 `big-mouth-cn/tv` 可用订阅：
  `https://raw.githubusercontent.com/big-mouth-cn/tv/main/iptv-ok.m3u`
- 启动后自动下载并解析 M3U 频道。
- 频道分组、频道列表、详情面板。
- 内置 Media3 / ExoPlayer 播放器。
- 遥控器操作：
  - OK：选择 / 播放
  - 播放中左键：上一个频道
  - 播放中右键：下一个频道
  - 返回：退出播放
- 支持添加自定义 HTTP/HTTPS M3U 订阅。
- 解析阶段过滤危险协议，只接受 `http`、`https`、`rtsp`、`rtmp`、`udp` 类型的播放地址。

## 盒子兼容性

最低支持 Android 6.0。应用同时声明普通 Android launcher 和 Android TV launcher，因此大多数可安装第三方 APK 的电视盒子都可以尝试安装。

部分盒子需要在系统设置里允许文件管理器安装未知来源 APK。

## 中国大陆网络说明

内置的是订阅 URL，不是把完整频道列表写死进 APK。首次打开时，App 会访问 GitHub raw 地址下载 M3U 文本。

在中国大陆网络环境下，`raw.githubusercontent.com` 可能无法访问或速度不稳定。如果打开后加载失败，可以在 App 里添加你自己的可访问 M3U 订阅地址，或把订阅文件放到局域网/NAS/自己的服务器上，再添加对应的 HTTP 地址。

## 安全策略

这个 App 只把 GitHub 订阅当作纯文本 M3U 读取：

- 不运行仓库代码。
- 不使用 WebView 加载直播页面。
- 不执行 JavaScript。
- 不允许 `file:`、`intent:`、`content:`、`data:`、`javascript:` 等协议进入播放列表。

## 构建方式

用 Android Studio 打开本目录，等待 Gradle 同步后构建运行。

本机当前命令行环境没有检测到 `gradle` 和 `ANDROID_HOME`，所以我没有在这里直接生成 APK。安装 Android Studio 后，它会自动使用项目中的 Gradle 配置同步依赖。

## GitHub Actions 打包 APK

项目已包含 `.github/workflows/android-debug-apk.yml`。推送到 GitHub 后，仓库会自动构建 debug APK：

1. 打开 GitHub 仓库的 `Actions` 页面。
2. 点最新一次 `Build Android APK`。
3. 在页面底部下载 `StreamBoxTV-debug-apk`。
4. 解压后得到 `app-debug.apk`，可改名为 `StreamBoxTV.apk`。
5. 拷到 U 盘，在电视盒子上安装。

## 后续建议

- 添加频道收藏和最近观看。
- 添加源健康检测和失败自动跳过。
- 添加手机扫码输入订阅地址，减少电视遥控器输入长 URL 的痛苦。
- 添加本地 M3U 文件导入。
