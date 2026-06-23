# Egern Sub2API Usage Widget

这个仓库现在包含六种用法：

- **Android App 版**：工程目录 `android-app/`，适合直接在安卓手机上安装使用，支持保存多个站点
- **PWA 多文件版**：入口 `index.html`，适合后续部署成静态网页并支持安装体验
- **单文件网页版**：入口 `index.single.html`，只有一个 HTML 文件，方便你单独拿出去部署或改名成 `index.html` 使用
- **同站页面注入版**：脚本文件 `site-panel.js`，适合你这种**不能改服务器、但可以先打开目标站点页面**的场景
- **油猴脚本版**：脚本文件 `sub2api-usage.user.js`，适合支持用户脚本的浏览器/扩展
- **Egern 小组件版**：原始版本，适合 iOS / Egern

## Android App 版

### 适合场景

- 想直接装到安卓手机上使用
- 想保存多个不同站点
- 每个站点都需要单独填写 `BASE_URL`、`EMAIL`、`PASSWORD`
- 不想再受浏览器跨域限制影响
- 想把用量信息放到安卓桌面

### 当前功能

- 添加 / 编辑 / 删除多个站点
- 每个站点单独查询今日用量
- 本地保存站点配置
- 展示总请求数、Token、消费、平均耗时
- 支持安卓桌面 Widget，给组件绑定某个站点后可点按刷新

### 工程位置

- `android-app/`

### 打开方式

1. 用 Android Studio 打开 `android-app`
2. 等 Gradle 同步完成
3. 连接安卓手机，或启动模拟器
4. 直接运行 `app`

### App 使用方法

1. 打开 App
2. 点右下角 `+`
3. 填写站点名称、`BASE_URL`、`EMAIL`、`PASSWORD`
4. 保存后，在站点卡片点击“查询”或右上角刷新按钮

### 桌面 Widget 使用方法

1. 先在 App 里至少添加一个站点
2. 回到安卓桌面，添加 `Sub2API 用量` 小组件
3. 选择要绑定的站点
4. 添加后点组件主体可刷新，点底部文字可打开 App

### GitHub 自动打包 APK

仓库已经包含工作流：

- `.github/workflows/android-apk.yml`

使用方法：

1. 把整个项目上传到你自己的 GitHub 仓库
2. 打开 GitHub 仓库页面的 `Actions`
3. 选择 `Build Android APK`
4. 点 `Run workflow`
5. 等待构建完成后，在该次运行页面底部下载产物：`sub2api-usage-debug-apk`
6. 解压后得到 `app-debug.apk`，传到手机安装即可

这个工作流会在 GitHub 上：

- 自动安装 Java 17
- 自动安装 Android SDK
- 自动安装独立 `Gradle 8.7`
- 直接执行 `gradle assembleDebug`
- **不依赖本仓库里的 `gradle-wrapper.jar`**

### 说明

- `BASE_URL` 填站点根地址，例如 `https://example.com`
- 不要填写 `/admin/usage`
- 如果账号开启了 2FA，当前 App 版也无法自动登录
- 配置保存在 App 私有目录文件 `sub2api-sites.json` 中

## 你当前最适合的版本

如果你的 Sub2API 站点是：

- `https://aiapi888.setbug.cn`

并且你：

- **不能改服务器**
- **不能开 CORS**
- **只能先打开这个网页再查询**

那么最适合你的就是：

- `sub2api-usage.user.js`
- `site-panel.js`
- `bookmarklet.txt`

这几个版本都不再从外部网页跨域请求接口，而是**直接在 `aiapi888.setbug.cn` 页面里注入查询面板**，请求走相对路径：

- `POST /api/v1/auth/login`
- `GET /api/v1/usage/dashboard/stats?timezone=...`

这样就能绕过前面那个浏览器跨域限制。

## 油猴脚本版

### 文件说明

- `sub2api-usage.user.js`：Tampermonkey / Violentmonkey 用户脚本

### 使用方法

1. 在支持用户脚本的浏览器里安装油猴类扩展
2. 导入 `sub2api-usage.user.js`
3. 打开 `https://aiapi888.setbug.cn`
4. 页面右下角会自动出现查询面板
5. 输入邮箱和密码后点击“保存并查询”

### 注意

- 这个脚本只匹配：`https://aiapi888.setbug.cn/*`
- 配置会保存在该站点的 `localStorage` 中
- 如果浏览器不支持用户脚本扩展，这个方案就无法使用

## 同站页面注入版

### 文件说明

- `site-panel.js`：可读版源码
- `bookmarklet.txt`：书签脚本内容，适合做成 bookmarklet

### 使用方法

1. 先打开：`https://aiapi888.setbug.cn`
2. 再运行书签脚本
3. 页面右下角会弹出一个查询面板
4. 输入邮箱和密码后点击“保存并查询”

### Edge 使用建议

手机版 Edge 对书签脚本支持不如桌面浏览器稳定。最省事的方式通常是：

1. 在桌面版 Edge 新建一个收藏夹书签
2. 把 `bookmarklet.txt` 里的整段内容粘贴到书签 URL
3. 登录同一个微软账号并同步收藏夹到手机
4. 手机上打开 `https://aiapi888.setbug.cn` 后，点击这个书签

### 这个版本保存了什么

同站页面注入版会把：

- 邮箱
- 密码

保存在当前站点的 `localStorage` 中，键名是：

- `sub2api-usage-site-panel-config`

如果你不想保留，可以在面板里点“清空本地配置”。

## 安卓网页版本

### 这是不是浏览器小组件？

可以理解为“**像小组件一样使用的网页应用**”：

- 可以在安卓浏览器中打开后 **添加到主屏幕**
- 添加后会像独立小应用一样启动
- 适合随手查看数据
- **不是** 安卓原生桌面小组件，不能保证像系统 Widget 一样常驻桌面实时展示

### 版本选择

#### 1. PWA 多文件版

使用入口：`index.html`

适合场景：

- 后续准备正式部署成静态网页
- 希望有安装体验
- 希望保留 `manifest` 和 `service worker`

相关文件：

- `index.html`
- `app.js`
- `styles.css`
- `manifest.webmanifest`
- `sw.js`
- `icon.svg`

#### 2. 单文件网页版

使用入口：`index.single.html`

适合场景：

- 只想保留一个文件
- 想自己随手部署
- 后续方便单独复制、改名、发到静态空间

这个版本已经把样式和逻辑都内联进一个文件里了。

### 如何使用

1. 选择一个版本：
   - 多文件版：打开 `index.html`
   - 单文件版：打开 `index.single.html`
2. 把对应文件部署到可访问的网址
3. 用安卓手机的 Edge 浏览器打开
4. 填写：

| 参数 | 说明 |
| --- | --- |
| `BASE_URL` | Sub2API 站点根地址，例如 `https://example.com`，不要填写 `/admin/usage` |
| `EMAIL` | Sub2API 登录邮箱 |
| `PASSWORD` | Sub2API 登录密码 |

5. 点击查询 / 刷新
6. 如果你愿意，可以再用 Edge 的“添加到主屏幕”

### 重要说明

安卓网页版本会直接从浏览器请求你的 Sub2API 接口，所以需要满足以下至少一项：

- **Sub2API 站点允许跨域请求（CORS）**
- 或者 **把网页部署在与你的 Sub2API 相同域名下**

如果不满足，浏览器可能会报“网络或跨域错误”。

### 本地开发预览

可以用任意静态文件服务器启动，例如：

```bash
npx serve .
```

然后访问页面进行测试。

## Egern 小组件版

Egern 小组件模块，用来查看 Sub2API 今天的用量：

- 总请求数
- 总 Token
- 实际消费
- 平均耗时

### 安装

在 Egern 中进入 `工具` -> `模块` -> 右上角 `+`，添加下面的模块 URL：

```text
https://raw.githubusercontent.com/ftufkc/egern-sub2api-usage-widget/main/sub2api-usage.module.yaml
```

保存后进入模块的 `Env` 配置，填写：

| 参数 | 说明 |
| --- | --- |
| `BASE_URL` | Sub2API 站点根地址，例如 `https://example.com`，不要填写 `/admin/usage` |
| `EMAIL` | Sub2API 登录邮箱 |
| `PASSWORD` | Sub2API 登录密码 |

然后进入 `分析` -> 左上角小组件画廊，选择 `Sub2API 今日用量`。添加到 iOS 主屏幕后，长按 Egern 小组件并在编辑界面选择这个小组件名称。

## 数据口径

每次刷新时会：

1. 请求 `POST /api/v1/auth/login` 登录；
2. 请求 `GET /api/v1/usage/dashboard/stats?timezone=...` 读取当前登录账号的今日 dashboard 统计；
3. 显示 `today_requests`、`today_tokens`、`today_actual_cost`、`average_duration_ms`，并在 Token/消费卡片中补充输入输出 Token 和标准消费。

当前脚本不再调用 admin 专用统计接口。管理员账号如果可以访问上述 dashboard 接口，会显示该账号在 dashboard 中返回的今日用量；不会读取平台全局 admin 统计。

脚本会携带当前系统时区；无法读取时默认使用 `Asia/Shanghai`。Egern 版请求约 10 分钟后刷新，PWA 版、单文件版、同站脚本版和油猴脚本版在页面打开期间也会定时刷新一次。

## 隐私

仓库不包含任何默认站点、账号或密码。

- Android App 版：配置保存在 App 私有目录文件中
- Egern 版：配置填写在模块 Env 中
- PWA 多文件版：配置保存在当前浏览器 `localStorage` 中
- 单文件网页版：配置也保存在当前浏览器 `localStorage` 中
- 同站页面注入版：配置保存在目标站点自己的 `localStorage` 中
- 油猴脚本版：配置同样保存在目标站点自己的 `localStorage` 中

如果账号启用了 2FA，小组件 / 网页版 / App 版都无法自动完成登录，会显示错误提示。

## 本地测试

```bash
npm test
```
