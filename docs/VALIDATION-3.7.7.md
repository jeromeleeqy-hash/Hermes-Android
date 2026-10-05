# 3.7.7 验证记录

验证日期：2026-10-04。基线：已交付的 Hermes Android 3.7.6 源码。

## 已完成

- 完整 `:app:testDebugUnitTest`：71 个测试类，389 项测试，0 失败、0 错误、0 跳过。包括 20 种卡片原生渲染、三种皮肤、大字体、滚动边界、过期表单保护、后台提交及对应服务器回执确认、工作区隔离、连接恢复与同步退避。
- Python 写入工具：10 项测试通过，覆盖严格卡片结构、20 类示例、原子写入、哈希冲突、路径边界、重复操作去重、回执保存、防伪造回执，以及定时整理不重开已关闭卡片。
- `:app:lintRelease`：0 Fatal、0 Error、660 Warning。警告仍保留，主要涉及既有资源和代码风格；不能描述成“零警告”。
- `:app:assembleRelease`：成功，单 DEX；APK ZIP 内容完整、16 KiB 原生库对齐检查通过。
- APK 签名 v2 验证通过，包名及签名与此前交付保持一致；versionCode=377、versionName=3.7.7，minSdk=26、targetSdk=36。
- 检查了自动化测试生成的原生首页、确认卡和方案比较卡截图。截图使用虚构示例内容，不能当作真实用户资料已经被重整的证明。

## 安装包信息

- 文件：Hermes-Android-3.7.7-release.apk
- 包名：com.qingyu.hermescompanion.preview
- 大小：64,895,543 字节
- APK SHA-256：07abc4b2645c0d6d16001c8fbe44241fddcb2703d442a48fefda9ef15ce03df8
- 签名证书 SHA-256：741c1844bb9267547e06af30d56c0da5bfcbf01491d3670496d88837a2860ad6

## 验证边界

尚未连接用户真实 Hermes 服务器执行配置、迁移或卡片动作，也未在用户手机上覆盖安装。实际网络故障来源仍须结合新版本保留的诊断记录确认；本次测试不能证明所有手机网络条件下都不会断线。

## 构建环境

Gradle 8.13、Java 21、Android SDK 36、Build Tools 36.0.0。Android 编译目标保持 Java 17；现有 backdrop 1.0.6 视觉组件的 JVM 类版本为 65，因此运行原生界面自动化测试需要 Java 21。

本轮先补齐官方构建/测试依赖，再用 Java 21 完整运行测试；没有跳过失败测试或降低校验要求。环境中的代理设置只用于下载依赖，不写入项目源码和 App 配置。

```sh
./gradlew :app:testDebugUnitTest :app:lintRelease :app:assembleRelease -PhermesPreview=true -PhermesSigningFile=/absolute/private/signing.keystore
python -m unittest discover -s tools -p 'test_today_writer.py' -v
```

签名使用此前保存的同一密钥；源码不包含签名私钥、用户实际 JSON、构建缓存或本地环境路径。
