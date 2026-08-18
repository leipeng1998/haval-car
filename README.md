# 哈弗 H6 车机空调控制定制版

本仓库基于 [leandrosavn/haval-impulse](https://github.com/leandrosavn/haval-impulse) 的 open source 项目进行中文定制，重点把空调虚拟仪表、空调面板和主菜单做成“空调控制优先”的中文车机工具，并保留实时数值、功能中心、主题系统等原有能力。

## 主要功能

- 空调仪表：虚拟空调面板，展示/操作风量、温度、自动、室外/车内温度等空调数据
- 空调主题：内置 Basic、Basic Light、Default 三套主题，均已汉化
- 实时数值：查看车机实时数据流
- 仪表投影：车机仪表数据投影相关功能
- 软件安装：在车机上安装其他应用
- 功能中心：聚合车机扩展功能
- 高级模式：Frida 调试入口等调试工具
- 主菜单：中文菜单，空调仪表置顶，优先触达

## 安装前提

- 第三代哈弗 H6 或兼容的 GWM 车机（建议先在模拟器或同型号车机上测试）
- 车机开启开发者模式 / ADB 调试
- 车机已安装并授权 Shizuku（项目依赖 Shizuku 调用系统隐藏能力）
- 能通过 ADB 安装 APK

> 不同车机固件、车型年份、屏幕分辨率可能影响布局和功能，部分功能需要高级权限才能使用。

## 使用 GitHub Actions 编译 APK

本仓库已配置 `Build and Release APK` 工作流，不需要本地安装 Android SDK 也能编译。

1. 把本仓库 Fork 到自己的 GitHub 账号
2. 打开 Fork 仓库的 **Actions** 页面
3. 选择 **Build and Release APK**，点击 **Run workflow**
4. 选择构建类型：
   - `debug`：默认选项，无需任何 Secrets，直接编译并上传 debug APK
   - `release`：需要先配置签名 Secrets，编译后还会创建 GitHub Release
5. 可选：填写 `version`，留空则自动读取 `v2.*` 标签并递增
6. 构建完成后，在本次运行的 **Summary / Artifacts** 下载 APK
   - debug：`haval-h6-debug-apk/app-debug.apk`
   - release：`haval-h6-release-apk/app-release.apk`

### Release 签名 Secrets

只有选择 `release` 时才需要配置以下仓库 Secrets：

| Secret | 说明 |
| --- | --- |
| `KEYSTORE_BASE64` | release keystore 文件的 Base64 文本 |
| `STORE_PASSWORD` | keystore 存储密码 |
| `KEY_ALIAS` | 签名别名 |
| `KEY_PASSWORD` | 别名对应密码 |

工作流会把 `KEYSTORE_BASE64` 解码为 `app/release.keystore`，再按 `app/build.gradle.kts` 的 release signingConfig 签名。首次使用建议选 `debug` 验证功能，确认可用后再配置 release 签名。

## 本地编译

环境需要 JDK 17 和 Android SDK：

```sh
./gradlew assembleDebug
```

输出：`app/build/outputs/apk/debug/app-debug.apk`

## 中文定制与本地化脚本

仓库内置了中文本地化脚本，会替换 Android 界面、空调主题和仪表组件中的葡语/英语界面文案，但会保留 `Normal`、`NORMAL`、`ALTO`、`BAIXO` 等状态枚举和数据值，避免破坏车机通信逻辑。

在 Windows PowerShell 5.1 下先运行：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\localize-zh.ps1
```

脚本是幂等的，重复运行不会产生重复替换；运行后再执行本地编译或推送到 GitHub 触发 Actions 即可得到中文版 APK。

## 相关文档

- 车机开发工具与 ADB 工作流：[tools/headunit-dev/README.md](tools/headunit-dev/README.md)
- 车机操作流程：[tools/headunit-dev/WORKFLOW.md](tools/headunit-dev/WORKFLOW.md)
- 空调主题构建：`cluster-widgets/basic`、`cluster-widgets/basic-light`、`cluster-widgets/default` 各自的 `README.md`

## 免责声明

本项目是独立开发的学习研究项目，与哈弗、GWM 及任何相关厂商无关联，未获得官方授权或认可。项目涉及对车机系统的逆向研究与实验，仅供学习研究使用，不用于商业用途，不保证与任何车型、固件版本的兼容性，不对安装后产生的数据丢失、功能异常或车辆问题承担责任。请自行评估风险并遵守当地法律与厂商条款。
