# HackerCNews

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

在开始使用之前，请修改以下配置文件：

1. `backend/src/main/resources/application.properties`:
   - 更新 MySQL 连接信息（数据库 URL、用户名和密码）
   - 更新 Redis 连接信息（如果不是本地默认配置）
   - 添加您的百度翻译 API 密钥

## 运行步骤

### 后端

1. 进入 backend 目录：`cd backend`
2. 构建项目：`mvn clean package`
3. 运行应用：`java -jar target/hacker-cnews-0.0.1-SNAPSHOT.jar`

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

## 未来计划

- 添加用户注册和登录功能
- 实现社区发帖和评论功能
- 优化前端界面，使用 Vue.js 框架
- 使用更高级的翻译服务提高翻译质量
