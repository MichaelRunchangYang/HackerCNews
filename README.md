# HackerCNews

(刚刚开始开发 2025.3.30)
HackerCNews 是一个将 Hacker News 内容翻译为中文并提供社区功能的平台。

## 项目结构

```
hacker-cnews-mvp/
├── backend/                       # 后端代码
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/hacker_cnews/
│   │   │   │   ├── config/           # 配置类
│   │   │   │   ├── controller/       # 控制器
│   │   │   │   ├── entity/           # 实体类
│   │   │   │   ├── repository/       # 数据访问
│   │   │   │   ├── service/          # 业务逻辑
│   │   │   │   └── HackerCNewsApplication.java  # 主应用类
│   │   │   └── resources/
│   │   │       ├── application.properties      # 配置文件
│   │   │       └── templates/                  # 模板文件（如有）
│   │   └── test/                               # 测试代码
│   └── pom.xml                                 # Maven配置
└── frontend/                      # 前端代码
    ├── index.html                 # 主页面
    ├── css/
    │   └── style.css              # 样式文件
    └── js/
        └── script.js              # JavaScript脚本
```

## 技术栈

- **后端**：Spring Boot 3.4.x, Java 17, Maven
- **数据库**：MySQL 8.0+
- **缓存**：Redis 6.x+
- **HTTP 客户端**：Spring WebFlux 的 WebClient
- **JSON 处理**：Jackson
- **翻译 API**：百度翻译开放平台基础版
- **前端**：HTML/CSS/JavaScript

## 环境准备

1. 安装 Java 17 或更高版本
2. 安装并启动 MySQL 8.0+
3. 安装并启动 Redis 6.x+
4. 注册百度翻译开放平台，获取 API 密钥

## 配置说明

本项目使用环境变量来管理敏感配置信息。在开始使用之前，请按照以下步骤进行配置：

1. 复制示例配置文件：

   ```bash
   cp backend/.env.properties.example backend/src/main/resources/.env.properties
   ```

2. 在复制的 `.env.properties` 文件中填入实际的配置值：

   ```
   # 数据库配置
   MYSQL_USERNAME=your_mysql_username
   MYSQL_PASSWORD=your_mysql_password

   # Redis配置
   REDIS_HOST=localhost
   REDIS_PORT=6379
   REDIS_PASSWORD=your_redis_password_if_any

   # 百度翻译API配置
   BAIDU_TRANSLATE_APPID=your_baidu_translate_appid
   BAIDU_TRANSLATE_KEY=your_baidu_translate_key
   ```

3. **重要安全提示**:
   - **永远不要**将含有实际密码、密钥等敏感信息的配置文件提交到版本控制系统
   - 确保 `.env.properties` 和 `.env` 文件已在 `.gitignore` 中排除
   - 在生产环境中，考虑使用环境变量或安全的密钥管理服务，而不是配置文件

## 运行步骤

### 后端

1. 进入 backend 目录：`cd backend`
2. 构建项目：`mvn clean package`
3. 设置环境变量（参见上方配置说明）
4. 运行应用：`java -jar target/hacker-cnews-0.0.1-SNAPSHOT.jar`

### 前端

1. 前端为静态文件，可以通过任何 HTTP 服务器提供，或简单地通过浏览器直接打开 frontend/index.html 文件

## 主要功能

- 从 Hacker News API 获取最新内容
- 将英文内容翻译为中文
- 将翻译后的内容存储到数据库中
- 通过前端页面展示翻译后的内容

## 注意事项

- 百度翻译 API 有调用频率和每日量限制，请合理使用
- 首次运行时，需要一些时间来获取和翻译 Hacker News 内容
- 不要将包含敏感信息的 `.env` 文件或 `.env.properties` 文件提交到公共代码库
- 项目已配置 `.gitignore` 以避免上述文件被意外提交

## 未来计划

- 添加用户注册和登录功能
- 实现社区发帖和评论功能
- 优化前端界面，使用 Vue.js 框架
- 使用更高级的翻译服务提高翻译质量
