document.addEventListener("DOMContentLoaded", () => {
  const newsList = document.getElementById("news-list");
  const loading = document.getElementById("loading");

  // 获取新闻数据
  fetch("/api/news")
    .then((response) => {
      if (!response.ok) {
        throw new Error("网络错误");
      }
      return response.json();
    })
    .then((news) => {
      loading.style.display = "none";

      if (news.length === 0) {
        newsList.innerHTML =
          '<li class="news-item">暂无新闻数据，请稍后再试</li>';
        return;
      }

      news.forEach((item) => {
        const li = document.createElement("li");
        li.className = "news-item";

        const title = document.createElement("h2");
        const link = document.createElement("a");

        // 使用中文标题，如果没有则使用英文标题
        link.textContent = item.titleZh || item.titleEn;

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

        // 格式化时间
        const date = new Date(item.time * 1000);
        const formattedDate = date.toLocaleString("zh-CN");

        meta.textContent = `类型: ${item.type} · 发布时间: ${formattedDate}`;
        li.appendChild(meta);

        newsList.appendChild(li);
      });
    })
    .catch((error) => {
      loading.style.display = "none";
      newsList.innerHTML = `<li class="news-item">加载失败: ${error.message}</li>`;
      console.error("Error fetching news:", error);
    });
});
