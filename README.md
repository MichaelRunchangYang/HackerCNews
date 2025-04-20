# HackerCNews

[![Status](https://img.shields.io/badge/Status-MVP%20Complete-green)](https://github.com/your-username/HackerCNews) <!-- 请替换为你的实际仓库链接 -->

HackerCNews 是一个旨在整合 [Hacker News](https://news.ycombinator.com/) 内容（并将其翻译为中文）与一个中文技术创业社区的平台。

**当前状态 (截至 2025-04-20):** 项目已基本完成 **MVP (最小可行产品)** 阶段。核心后端功能，包括从 Hacker News 获取数据、使用 DeepL API 进行翻译、存储到 MySQL 数据库以及使用 Redis 进行缓存，均已实现并可运行。前端提供了一个基本的界面来展示翻译后的新闻列表（存在服务器端渲染和客户端渲染两种方式）。

## 主要功能 (MVP)

- 定时从 Hacker News API 获取最新的 Top Stories。
- 使用 DeepL API 将英文标题和内容（如果存在）翻译为中文。
- 将获取到的原始信息和翻译后的内容存储在 MySQL 数据库中。
- 使用 Redis 缓存新闻条目和翻译结果，以提高性能并减少 API 调用。
- 提供 REST API (`/api/news`) 以获取新闻列表和详情。
- 提供一个基本的前端页面来展示翻译后的新闻列表。
  - 访问 `/` (根路径) 使用后端服务器端渲染 (SSR)。
  - 访问 `/index.html` 使用客户端渲染 (CSR)。

## 技术栈 (MVP 实际使用)

- **后端**:
  - Java 17
  - Spring Boot 3.4.4
  - Spring WebFlux (for asynchronous operations & WebClient)
  - Spring Data JPA (with Hibernate)
  - Spring Data Redis
  - Maven
- **数据库**: MySQL 8.0+
- **缓存**: Redis 6.x+ (单机模式)
- **翻译服务**: DeepL API (Free or Pro)
- **HTTP 客户端**: Spring WebFlux `WebClient`
- **JSON 处理**: Jackson
- **前端**:
  - HTML5
  - CSS3
  - Vanilla JavaScript (ES6+)

## 项目结构

```
hacker-cnews/
├── backend/                       # 后端 Spring Boot 应用
│   ├── src/main/java/...          # Java 源代码 (Config, Controller, Service, Repository, Entity...)
│   ├── src/main/resources/        # 资源文件 (application.properties, .env.properties)
│   └── pom.xml                    # Maven 构建文件
├── frontend/                      # 客户端渲染的前端文件
│   ├── index.html
│   ├── css/style.css
│   └── js/script.js
├── .env                           # 环境变量文件 (用于存储敏感信息)
├── ImplementationGuide.md         # (内部)详细设计与迭代计划文档
├── longtermmemory.md              # (内部)AI 代理使用的代码分析文档
├── README.md                      # 本文件
└── .gitignore
└── LICENSE                        # 项目许可证文件
```

## 环境准备

在运行项目之前，请确保你的开发环境中安装了以下软件：

1.  **Java Development Kit (JDK)**: 版本 17 或更高。
2.  **Maven**: 用于构建后端项目。
3.  **MySQL Server**: 版本 8.0 或更高。
4.  **Redis Server**: 版本 6.x 或更高。
5.  **DeepL API Key**: 你需要一个 DeepL API 密钥 (Free 或 Pro 版本均可)。可以在 [DeepL 官网](https://www.deepl.com/pro-api) 注册获取。

## 配置说明

本项目的敏感配置（如数据库密码、API 密钥）通过根目录下的 `.env` 文件进行管理。后端应用启动时会加载此文件。

1.  **创建 `.env` 文件**:
    在项目根目录下创建一个名为 `.env` 的文件。

2.  **编辑 `.env` 文件**:
    根据以下模板，在 `.env` 文件中填入你的实际配置值：

    ```dotenv
    # MySQL Database Configuration
    MYSQL_USERNAME=your_mysql_username
    MYSQL_PASSWORD=your_mysql_password
    # 注意: 数据库 URL 和驱动在 backend/src/main/resources/application.properties 中配置
    # 默认数据库名是 'hacker_cnews'，如果不存在会自动创建

    # Redis Configuration
    REDIS_HOST=localhost
    REDIS_PORT=6379
    # 如果你的 Redis 设置了密码，请取消下一行的注释并填入密码
    # REDIS_PASSWORD=your_redis_password

    # DeepL API Configuration
    # 请确保使用你的有效 DeepL API Key (Free 或 Pro)
    DEEPL_API_KEY=your_deepl_api_key
    # DeepL API URL 在 application.properties 中配置，默认为免费版 URL
    # 如果使用 Pro 版，请修改 application.properties 中的 deepl.translate.url
    ```

3.  **安全提示**:
    - `.env` 文件已被添加到 `.gitignore` 中，**切勿**将其提交到版本控制系统（如 Git）。
    - 在生产环境中，推荐使用操作系统的环境变量或更安全的密钥管理服务来传递这些敏感配置，而不是依赖 `.env` 文件。

## 运行步骤

### 1. 后端 (Spring Boot Application)

1.  **导航到后端目录**:
    ```bash
    cd backend
    ```
2.  **构建项目**:
    使用 Maven 构建项目。这会下载依赖并编译代码。
    ```bash
    mvn clean package -DskipTests
    ```
    _(`-DskipTests` 会跳过测试，加快首次构建速度)_
3.  **运行应用**:
    确保你的 MySQL 和 Redis 服务正在运行，并且 `.env` 文件已在项目根目录配置好。

    ```bash
    # 从 backend 目录运行
    java -jar target/hacker-cnews-0.0.1-SNAPSHOT.jar
    ```

    或者，如果你在 IDE 中运行，可以直接运行 `HackerCNewsApplication.java` 类。

    后端服务默认运行在 `http://localhost:8080`。

### 2. 前端

- 直接在浏览器中访问后端服务的根路径：
  [http://localhost:8080/](http://localhost:8080/)
  这个页面是由后端 `IndexController` 动态生成的。

## 注意事项

- **API 限制**: DeepL 的免费 API 有调用频率和字符数限制。如果遇到翻译问题，请检查你的 API 使用量。
- **首次运行**: 应用首次启动时，需要一些时间来获取和翻译 Hacker News 的初始内容，数据库和页面可能需要几分钟才会填充数据。你可以通过访问 `/api/test/dashboard` (如果测试控制器启用) 查看后台任务状态。
- **测试端点**: 项目包含一个非常详细的 `TestController` (`/api/test/*`)，用于开发和调试。**在生产环境中部署时，务必禁用或移除这些测试端点**，因为它们可能暴露敏感信息或允许执行危险操作（如清空数据库）。
- **`.env` 文件安全**: 再次强调，不要将包含真实密钥和密码的 `.env` 文件上传到任何公共仓库。

## 未来计划 (基于 ImplementationGuide.md)

- **迭代一**: 添加用户系统 (注册/登录 - Spring Security)、社区功能 (发帖/评论)，并使用 Vue.js 重构前端。
- **迭代二**: 探索将后端重构为微服务架构 (Spring Cloud Alibaba)、使用消息队列 (RabbitMQ) 进行异步处理、并集成 AI 代理框架 (LangChain4j) 优化翻译流程。
- **其他**: 性能优化、搜索功能 (Elasticsearch)、实时通知等。

## 贡献

欢迎对本项目做出贡献！请在提交 Pull Request 前先开一个 Issue 讨论你的想法。

## 许可证

本项目采用 [MIT License](LICENSE) 授权。
