document.addEventListener("DOMContentLoaded", () => {
  const newsList = document.getElementById("news-list");
  const loading = document.getElementById("loading");

  console.log("正在加载新闻数据...");

  // 获取新闻数据
  fetch("/api/news")
    .then((response) => {
      if (!response.ok) {
        throw new Error("网络错误");
      }
      return response.json();
    })
    .then((news) => {
      console.log("获取到新闻数据:", news);
      loading.style.display = "none";

      if (!news || news.length === 0) {
        newsList.innerHTML =
          '<li class="news-item">暂无新闻数据，请稍后再试</li>';
        return;
      }

      // 过滤掉没有标题的无效记录
      const validNews = news.filter(
        (item) =>
          (item.titleZh && item.titleZh.trim()) ||
          (item.titleEn && item.titleEn.trim())
      );

      console.log("有效的新闻条目:", validNews.length);

      if (validNews.length === 0) {
        newsList.innerHTML =
          '<li class="news-item">暂无有效的新闻数据，请稍后再试</li>';
        return;
      }

      validNews.forEach((item) => {
        const li = document.createElement("li");
        li.className = "news-item";

        const title = document.createElement("h2");
        const link = document.createElement("a");

        // 使用中文标题，如果没有则使用英文标题
        link.textContent =
          item.titleZh && item.titleZh.trim()
            ? item.titleZh
            : item.titleEn && item.titleEn.trim()
            ? item.titleEn
            : "无标题新闻";

        // 设置链接地址
        if (item.url) {
          link.href = item.url;
          link.target = "_blank";
        } else {
          // 如果没有外部链接，可以链接到内部详情页
          link.href = `/news/${item.id}`;
        }

        title.appendChild(link);
        li.appendChild(title);

        // 添加元数据
        const meta = document.createElement("div");
        meta.className = "news-meta";

        // 安全地格式化时间
        let timeInfo = "发布时间: 未知";
        if (item.time) {
          try {
            const date = new Date(item.time * 1000);
            const formattedDate = date.toLocaleString("zh-CN");
            timeInfo = `发布时间: ${formattedDate}`;
          } catch (e) {
            console.error("时间格式化错误:", e);
          }
        }

        // 安全地获取类型
        const typeInfo = item.type ? `类型: ${item.type}` : "类型: 未知";

        meta.textContent = `${typeInfo} · ${timeInfo}`;
        li.appendChild(meta);

        newsList.appendChild(li);
      });
    })
    .catch((error) => {
      console.error("加载新闻失败:", error);
      loading.style.display = "none";
      newsList.innerHTML = `<li class="news-item">加载失败: ${error.message}</li>`;
    });
});
