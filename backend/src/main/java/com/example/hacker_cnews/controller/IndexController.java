package com.example.hacker_cnews.controller;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.hacker_cnews.entity.NewsItem;
import com.example.hacker_cnews.repository.NewsItemRepository;

@RestController
public class IndexController {

    @Autowired
    private NewsItemRepository newsItemRepository;

    @GetMapping(value = "/", produces = "text/html")
    public String index() {
        try {
            // 按排名(rank)获取新闻 - 排名越小越靠前
            List<NewsItem> newsItems = newsItemRepository.findAll(
                Sort.by(Sort.Direction.ASC, "rank")
                    .and(Sort.by(Sort.Direction.DESC, "lastUpdated"))
            );
            
            // 过滤有效数据并去除重复排名
            Map<Integer, Boolean> rankProcessed = new HashMap<>();
            newsItems = newsItems.stream()
                .filter(item -> item.getTitleEn() != null && !item.getTitleEn().isEmpty())
                .filter(item -> {
                    // 跳过null排名
                    if (item.getRank() == null) {
                        return false;
                    }
                    // 如果这个排名已经处理过，则跳过
                    if (rankProcessed.containsKey(item.getRank())) {
                        return false;
                    }
                    // 标记这个排名已处理
                    rankProcessed.put(item.getRank(), true);
                    return true;
                })
                .limit(30)
                .collect(Collectors.toList());

            // 计算最近获取的新闻时间
            long totalItems = newsItemRepository.count();
            String lastUpdated = "未知";
            if (!newsItems.isEmpty()) {
                Instant latestTime = null;
                // 防止空指针异常
                if (newsItems.get(0).getLastUpdated() != null) {
                    latestTime = newsItems.get(0).getLastUpdated();
                } else if (newsItems.get(0).getCreatedAt() != null) {
                    latestTime = newsItems.get(0).getCreatedAt();
                } else {
                    latestTime = Instant.now();
                }
                
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withLocale(Locale.CHINA)
                    .withZone(ZoneId.systemDefault());
                lastUpdated = formatter.format(latestTime);
            }

            // 开始构建HTML
            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html>\n");
            html.append("<html lang=\"zh-CN\">\n");
            html.append("<head>\n");
            html.append("    <meta charset=\"UTF-8\">\n");
            html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
            html.append("    <title>Hacker C News - 中文黑客新闻</title>\n");
            html.append("    <style>\n");
            html.append("        body {\n");
            html.append("            font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif;\n");
            html.append("            line-height: 1.6;\n");
            html.append("            color: #333;\n");
            html.append("            max-width: 900px;\n");
            html.append("            margin: 0 auto;\n");
            html.append("            padding: 20px;\n");
            html.append("            background-color: #f6f6ef;\n");
            html.append("        }\n");
            html.append("        header {\n");
            html.append("            background-color: #ff6600;\n");
            html.append("            padding: 10px 20px;\n");
            html.append("            margin-bottom: 20px;\n");
            html.append("            border-radius: 4px;\n");
            html.append("            color: white;\n");
            html.append("            display: flex;\n");
            html.append("            justify-content: space-between;\n");
            html.append("            align-items: center;\n");
            html.append("        }\n");
            html.append("        header h1 {\n");
            html.append("            margin: 0;\n");
            html.append("            font-size: 1.5em;\n");
            html.append("        }\n");
            html.append("        .news-item {\n");
            html.append("            padding: 10px 5px;\n");
            html.append("            border-bottom: 1px solid #e2e2e2;\n");
            html.append("        }\n");
            html.append("        .news-title {\n");
            html.append("            font-size: 18px;\n");
            html.append("            font-weight: 500;\n");
            html.append("            margin-bottom: 5px;\n");
            html.append("        }\n");
            html.append("        .news-title a {\n");
            html.append("            color: #000;\n");
            html.append("            text-decoration: none;\n");
            html.append("        }\n");
            html.append("        .news-title a:hover {\n");
            html.append("            text-decoration: underline;\n");
            html.append("        }\n");
            html.append("        .news-meta {\n");
            html.append("            font-size: 14px;\n");
            html.append("            color: #777;\n");
            html.append("        }\n");
            html.append("        .original-title {\n");
            html.append("            font-size: 14px;\n");
            html.append("            color: #666;\n");
            html.append("            margin-top: 5px;\n");
            html.append("            font-style: italic;\n");
            html.append("        }\n");
            html.append("        .empty-notice {\n");
            html.append("            text-align: center;\n");
            html.append("            padding: 20px;\n");
            html.append("            background: #e9e9e9;\n");
            html.append("            border-radius: 4px;\n");
            html.append("        }\n");
            html.append("        .refresh-section {\n");
            html.append("            background-color: #e2e2e2;\n");
            html.append("            padding: 10px;\n");
            html.append("            margin: 10px 0;\n");
            html.append("            border-radius: 4px;\n");
            html.append("            display: flex;\n");
            html.append("            justify-content: space-between;\n");
            html.append("        }\n");
            html.append("        .refresh-btn {\n");
            html.append("            background: #ff6600;\n");
            html.append("            color: white;\n");
            html.append("            border: none;\n");
            html.append("            padding: 5px 15px;\n");
            html.append("            cursor: pointer;\n");
            html.append("            border-radius: 4px;\n");
            html.append("        }\n");
            html.append("        footer {\n");
            html.append("            margin-top: 30px;\n");
            html.append("            text-align: center;\n");
            html.append("            font-size: 14px;\n");
            html.append("            color: #888;\n");
            html.append("        }\n");
            html.append("    </style>\n");
            html.append("</head>\n");
            html.append("<body>\n");
            html.append("    <header>\n");
            html.append("        <h1>Hacker C News - 中文黑客新闻</h1>\n");
            html.append("    </header>\n");
            
            // 刷新区域
            html.append("    <div class=\"refresh-section\">\n");
            html.append("        <div>\n");
            html.append("            <span>最后更新: ").append(lastUpdated).append("</span><br>\n");
            html.append("            <span>数据库中的新闻数量: ").append(totalItems).append("</span>\n");
            html.append("        </div>\n");
            html.append("        <div>\n");
            html.append("            <a href=\"/api/test/manual-update\" target=\"_blank\"><button class=\"refresh-btn\">获取最新新闻</button></a>\n");
            html.append("            <button class=\"refresh-btn\" onclick=\"window.location.reload()\">刷新页面</button>\n");
            html.append("        </div>\n");
            html.append("    </div>\n");

            html.append("    <main>\n");
            
            if (newsItems.isEmpty()) {
                html.append("        <div class=\"empty-notice\">\n");
                html.append("            <p>暂无新闻数据，请点击获取最新新闻按钮来获取新闻。</p>\n");
                html.append("        </div>\n");
            } else {
                for (NewsItem item : newsItems) {
                    html.append("        <div class=\"news-item\">\n");
                    
                    // 使用中文标题，如果没有则使用英文标题
                    String title = (item.getTitleZh() != null && !item.getTitleZh().isEmpty()) 
                        ? item.getTitleZh() 
                        : item.getTitleEn();
                    
                    html.append("            <div class=\"news-title\">\n");
                    html.append("                <a href=\"").append(item.getUrl() != null ? item.getUrl() : "#").append("\" target=\"_blank\">")
                        .append(title).append("</a>\n");
                    html.append("            </div>\n");
                    
                    // 如果有中文标题，则显示原始英文标题
                    if (item.getTitleZh() != null && !item.getTitleZh().isEmpty()) {
                        html.append("            <div class=\"original-title\">")
                            .append(item.getTitleEn()).append("</div>\n");
                    }
                    
                    // 元数据显示
                    String formattedTime = "";
                    if (item.getTime() != null) {
                        long timestamp = item.getTime() * 1000; // 转换为毫秒
                        Instant instant = Instant.ofEpochMilli(timestamp);
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                            .withLocale(Locale.CHINA)
                            .withZone(ZoneId.systemDefault());
                        formattedTime = formatter.format(instant);
                    }
                    
                    html.append("            <div class=\"news-meta\">")
                        .append("排名: ").append(item.getRank() != null ? item.getRank() : "未知")
                        .append(" | 分数: ").append(item.getScore() != null ? item.getScore() : "未知")
                        .append(" | 类型: ").append(item.getType() != null ? item.getType() : "未知")
                        .append(" | 发布时间: ").append(!formattedTime.isEmpty() ? formattedTime : "未知")
                        .append(" | ID: ").append(item.getId())
                        .append("</div>\n");
                        
                    html.append("        </div>\n");
                }
            }
            
            html.append("    </main>\n");
            html.append("    <footer>\n");
            html.append("        <p>数据来源: <a href=\"https://news.ycombinator.com\" target=\"_blank\">Hacker News</a> | 使用Spring Boot & Redis构建</p>\n");
            html.append("    </footer>\n");
            html.append("</body>\n");
            html.append("</html>\n");
            
            return html.toString();
        } catch (Exception e) {
            // 捕获任何异常，显示友好的错误页面
            StringBuilder errorHtml = new StringBuilder();
            errorHtml.append("<!DOCTYPE html>\n");
            errorHtml.append("<html lang=\"zh-CN\">\n");
            errorHtml.append("<head>\n");
            errorHtml.append("    <meta charset=\"UTF-8\">\n");
            errorHtml.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
            errorHtml.append("    <title>Hacker C News - 错误</title>\n");
            errorHtml.append("    <style>\n");
            errorHtml.append("        body {\n");
            errorHtml.append("            font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif;\n");
            errorHtml.append("            line-height: 1.6;\n");
            errorHtml.append("            color: #333;\n");
            errorHtml.append("            max-width: 900px;\n");
            errorHtml.append("            margin: 0 auto;\n");
            errorHtml.append("            padding: 20px;\n");
            errorHtml.append("            background-color: #f6f6ef;\n");
            errorHtml.append("        }\n");
            errorHtml.append("        .error-container {\n");
            errorHtml.append("            background-color: #fff3cd;\n");
            errorHtml.append("            border: 1px solid #ffeeba;\n");
            errorHtml.append("            padding: 20px;\n");
            errorHtml.append("            border-radius: 5px;\n");
            errorHtml.append("            margin: 20px 0;\n");
            errorHtml.append("        }\n");
            errorHtml.append("        h1 {\n");
            errorHtml.append("            color: #cc0000;\n");
            errorHtml.append("        }\n");
            errorHtml.append("    </style>\n");
            errorHtml.append("</head>\n");
            errorHtml.append("<body>\n");
            errorHtml.append("    <div class=\"error-container\">\n");
            errorHtml.append("        <h1>系统暂时无法处理您的请求</h1>\n");
            errorHtml.append("        <p>很抱歉，系统遇到了一个问题。您可以尝试以下操作：</p>\n");
            errorHtml.append("        <ul>\n");
            errorHtml.append("            <li><a href=\"/api/test/create-test-data\">创建测试数据</a></li>\n");
            errorHtml.append("            <li><a href=\"/api/test/manual-update\">手动更新数据</a></li>\n");
            errorHtml.append("            <li><a href=\"/\">刷新页面</a></li>\n");
            errorHtml.append("        </ul>\n");
            errorHtml.append("        <p>错误信息: ").append(e.getMessage()).append("</p>\n");
            errorHtml.append("    </div>\n");
            errorHtml.append("</body>\n");
            errorHtml.append("</html>\n");
            
            return errorHtml.toString();
        }
    }
} 