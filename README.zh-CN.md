[English](README.md) | 简体中文

<p align="center">
  <img src="docs/images/app-icon.png" alt="Collins Dictionary" width="128" />
</p>

# Collins Dictionary

> www.collinsdictionary.com © HarperCollins Publishers Ltd 2025
>
> 上述归属声明系 [Collins API 使用条款](https://blog.collinsdictionary.com/terms-conditions-collins-api/) 所要求。
> 本应用未获 HarperCollins Publishers 的推广、支持或赞助。

[![Release](https://github.com/Black-Cyan/collins-dictionary/actions/workflows/release.yml/badge.svg)](https://github.com/Black-Cyan/collins-dictionary/actions/workflows/release.yml)
[![Latest Release](https://img.shields.io/github/v/release/Black-Cyan/collins-dictionary)](https://github.com/Black-Cyan/collins-dictionary/releases/latest)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)

基于 Collins Dictionary API 构建的跨平台桌面端和 Android 端词典应用。使用
Kotlin Multiplatform 和 Compose Multiplatform 开发，从同一套共享代码库出发，为
Linux 和 Android 提供原生体验。

## 应用截图

<table>
  <tr>
    <td><img src="docs/images/home.jpg" alt="首页" /></td>
    <td><img src="docs/images/entry-detail.jpg" alt="词条详情" /></td>
    <td><img src="docs/images/settings.jpg" alt="设置" /></td>
  </tr>
</table>

## 功能特性

- 全文词典查询，提供完整词条信息
- 发音音频播放
- 例句与用法说明
- 书签功能，支持离线访问已查阅词条
- 自动暗色模式，跟随系统主题
- 跨平台：Linux（.deb / .rpm / .tar.gz）和 Android

## 获取 Collins API Key

应用需要 Collins Dictionary API Key 才能使用。获取方式如下：

1. 访问 <https://www.collinsdictionary.com/api>，点击底部的 **fill in this form**（或页面上的等效链接）。
2. 填写并提交 API 访问申请表。
3. 等待 Collins 发送确认邮件，确认已收到你的申请。
4. 回复该邮件，即可获得 API Key。

获得 Key 后，打开应用进入 **Settings > API Configuration** 填入；或在启动前设置
环境变量 `COLLINS_ACCESS_KEY`。

## 下载

各版本预构建安装包可在
[Releases](https://github.com/Black-Cyan/collins-dictionary/releases/latest)
页面获取。

| 平台    | 格式            | 说明                        |
|---------|-----------------|-----------------------------|
| Linux   | `.deb` (x64)    | Debian、Ubuntu 及衍生发行版 |
| Linux   | `.rpm` (x64)    | Fedora、RHEL 及衍生发行版   |
| Linux   | `.tar.gz` (x64) | 解压即用；已内置 JRE        |
| Android | `.apk`          | Android 8.0+（API 26）      |

### Arch Linux（AUR）

AUR 包 `collins-dictionary-bin` 的维护文件位于本仓库 `packaging/aur/` 目录下。
AUR 注册目前处于关闭状态，可从 PKGBUILD 本地构建。详见
[`packaging/aur/README.md`](packaging/aur/README.md)。

## 从源码构建

### 前置条件

- JDK 21+（构建系统使用 [JetBrains Runtime](https://www.jetbrains.com/jbrs/)
  自动配置工具链，无需手动安装 JBR）

### 桌面端

```bash
./gradlew :desktopApp:run
```

构建可分发包（含内置 JRE）：

```bash
./gradlew :desktopApp:createDistributable
```

产物位于 `desktopApp/build/compose/binaries/main/app/`。

### Android

构建 Release APK 前需设置签名环境变量：

```bash
export KEYSTORE_BASE64=<base64 编码的 keystore>
export KEY_ALIAS=<别名>
export KEY_PASSWORD=<密钥密码>
export KEYSTORE_PASSWORD=<keystore 密码>
```

构建：

```bash
./gradlew :androidApp:assembleRelease
```

Debug 构建（无需签名）：

```bash
./gradlew :androidApp:assembleDebug
```

## 技术栈

| 层级        | 技术                       |
|-------------|----------------------------|
| 语言        | Kotlin（Multiplatform）    |
| UI          | Compose Multiplatform 1.11 |
| 网络        | Ktor（OkHttp 引擎）        |
| 序列化      | kotlinx.serialization      |
| 音频（JVM） | JLayer                     |
| 图片加载    | Coil 3                     |
| 配置存储    | multiplatform-settings     |
| 构建系统    | Gradle 9.1 + Kotlin DSL    |
| CI          | GitHub Actions             |

## 参与贡献

欢迎贡献代码。提交 Pull Request 之前，请先开 Issue 讨论拟议的变更。

1. Fork 本仓库。
2. 创建功能分支（`git checkout -b my-feature`）。
3. 提交变更。
4. 推送分支（`git push origin my-feature`）。
5. 创建 Pull Request。

## 贡献者

[![Contributors](https://contrib.rocks/image?repo=Black-Cyan/collins-dictionary)](https://github.com/Black-Cyan/collins-dictionary/graphs/contributors)

查看完整[贡献者列表](https://github.com/Black-Cyan/collins-dictionary/graphs/contributors)。

## 许可协议

本项目基于 [GNU General Public License v3.0](https://www.gnu.org/licenses/gpl-3.0.html)
许可协议发布。

完整协议文本见 [LICENSE](LICENSE)。
