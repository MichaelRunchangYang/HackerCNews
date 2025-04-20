# HackerCNews Project - Long Term Memory (Generated YYYY-MM-DD)

## 1. Project Overview & Current State

- **Purpose:** Build a platform ("HackerCNews") combining Hacker News content aggregation/translation (EN to ZH) and a Chinese tech/startup community forum.
- **Current State (as of analysis):** **MVP (Minimum Viable Product) stage largely complete.** The core backend functionality for fetching, translating, storing, and caching Hacker News data is implemented and functional. A basic frontend exists in two forms (SSR and CSR).
- **Next Steps Planned (Based on ImplementationGuide.md):** Iteration 1 (User Community Features, Vue.js frontend, Spring Security) and Iteration 2 (Microservices, AI Agent Optimization, Spring Cloud, RabbitMQ, LangChain4j) have **not** yet started.

## 2. Technology Stack (Actual - MVP)

- **Backend Framework:** Spring Boot 3.4.4
- **Language:** Java 17
- **Build Tool:** Maven
- **Database:** MySQL 8.0+ (via `spring-boot-starter-data-jpa`, `mysql-connector-j`)
- **Cache:** Redis 6.x+ (Single Node) (via `spring-boot-starter-data-redis`)
- **HTTP Client (Async):** Spring WebFlux `WebClient` (for HN & DeepL APIs)
- **JSON Processing:** Jackson (with `JavaTimeModule`)
- **Translation Service:** DeepL API (via `TranslationService`)
- **Configuration Loading:** `.env` file loaded via custom `PropertiesConfig` and `${...}` placeholders in `application.properties`.
- **Frontend:**
  - Option 1 (CSR): Pure HTML (`frontend/index.html`), CSS (`frontend/css/style.css`), JavaScript (`frontend/js/script.js`)
  - Option 2 (SSR): Backend `IndexController` generating HTML directly.
- **Testing/Debugging:** Extensive custom endpoints in `TestController`.
- **Missing Planned Tech:** Spring Security, Spring Cloud (Nacos, Gateway, Sentinel, etc.), RabbitMQ, LangChain4j, Vue.js.

## 3. Project Structure

```
hacker-cnews/
├── backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/example/hacker_cnews/
│   │   │   │       ├── config/       # Spring/App configuration classes
│   │   │   │       ├── controller/   # Handles HTTP requests (API & SSR)
│   │   │   │       ├── entity/       # JPA Entities (Database mapping)
│   │   │   │       ├── repository/   # Spring Data JPA interfaces
│   │   │   │       ├── service/      # Business logic
│   │   │   │       ├── util/         # Utility classes
│   │   │   │       ├── dao/          # (Currently empty) Data Access Objects
│   │   │   │       ├── common/       # (Currently empty) Common classes/structs
│   │   │   │       └── HackerCNewsApplication.java # Main entry point
│   │   │   └── resources/
│   │   │       ├── application.properties # Main config file
│   │   │       └── templates/             # (Likely unused for HTML, maybe email later)
│   │   └── test/
│   └── pom.xml                  # Maven build file
├── frontend/                    # Client-side rendering files
│   ├── index.html
│   ├── css/
│   │   └── style.css
│   └── js/
│       └── script.js
├── .env                         # Environment variables (sensitive config)
├── ImplementationGuide.md       # Project planning document
├── longtermmemory.md            # This file
├── README.md
└── ... (other config files like .gitignore)
```

## 4. Detailed Backend Module Analysis

### 4.1 `config` Package

- **`RedisConfig.java`**:
  - Defines two `RedisTemplate` beans:
    - `redisTemplate` (String, String): For simple key-value caching (e.g., translation cache). Uses `StringRedisSerializer`.
    - `newsItemRedisTemplate` (String, NewsItem): For caching full `NewsItem` objects. Uses `StringRedisSerializer` for keys and `Jackson2JsonRedisSerializer` for values. The Jackson serializer is configured with `JavaTimeModule` and default typing enabled (`activateDefaultTyping`) for correct object reconstruction.
- **`WebFluxConfig.java`**:
  - Implements `WebFluxConfigurer`.
  - Maps unhandled requests (`/**`) to serve static files from the `file:frontend/` directory.
- **`JacksonConfig.java`**:
  - Defines a global `ObjectMapper` bean.
  - Registers `JavaTimeModule` for proper Java 8+ time type serialization/deserialization.
- **`PropertiesConfig.java`**:
  - **Custom implementation** to load properties from a `.env.properties` file (relative to classpath) during startup (`@PostConstruct`).
  - Adds these properties to the Spring `Environment` and also sets them as System properties (`System.setProperty`).
  - Includes logic to filter sensitive keys (password, secret, key, etc.) before logging loaded keys. Appears to be the active mechanism for loading `.env` content, potentially replacing/supplementing `spring-dotenv`.
- **`DotenvConfig.java`**:
  - **Deprecated**. Marked as replaced by `PropertiesConfig`. Previously used `@PropertySource` to load `.env.properties`. Kept possibly for compatibility.
- **`HackerNewsConfig.java`**:
  - A `@Configuration` class using `@Value` to inject Hacker News specific properties from `application.properties` (`items.limit`, `max-stored-items`, `poll.interval`). Provides type-safe access to these configurations for other beans.

### 4.2 `entity` Package

- **`NewsItem.java`**:
  - JPA Entity (`@Entity`) mapped to the `translated_news` table (`@Table`).
  - Implements `Serializable` (needed for caching).
  - Primary Key (`@Id`): `id` (Long), value provided by application (HN Item ID), **not** auto-generated.
  - Fields:
    - `titleEn`, `titleZh` (String, length 500)
    - `url` (String, length 1000)
    - `textEn`, `textZh` (String, TEXT column)
    - `commentIds` (String, JSON column definition, stores list of comment IDs)
    - `time` (Long, HN Unix timestamp)
    - `type` (String, length 50)
    - `createdAt` (Instant)
    - `score` (Integer): HN score.
    - `rank` (Integer): Recent HN ranking (`@Column(name = "`rank`")` to avoid keyword conflict).
    - `lastUpdated` (Instant): Timestamp of last update in local DB.
  - Uses standard getters/setters (no Lombok here).
  - No entities for User, Post, Comment yet.

### 4.3 `repository` Package

- **`NewsItemRepository.java`**:
  - Spring Data JPA interface (`@Repository`) extending `JpaRepository<NewsItem, Long>`.
  - Provides standard CRUD methods automatically.
  - Custom query methods derived from name:
    - `findTop30ByOrderByTimeDesc()`: Get top 30 news items ordered by time descending.
    - `findTop50ByOrderByTimeDesc()`: Get top 50 news items ordered by time descending (used by `NewsController`).
  - No repositories for User, Post, Comment yet.

### 4.4 `service` Package

- **`HackerNewsService.java`**:
  - Responsible for interacting with the Hacker News Firebase API (`https://hacker-news.firebaseio.com/v0`).
  - Uses `WebClient` configured with:
    - Increased timeouts (connect: 60s, response/read/write: 120s) via underlying Netty `HttpClient`.
    - Request/Response logging filters (DEBUG level).
  - `getTopStories(int limit)`:
    - Calls `/topstories.json`.
    - Returns `Mono<List<Long>>`.
    - Includes timeout (`120s`) and exponential backoff retry (`Retry.backoff(3, ...)`).
    - Handles empty or error responses gracefully.
  - `getItemById(Long id)`:
    - Calls `/item/{id}.json`.
    - **Robust implementation:** Fetches response as `String` first, checks for null/empty, then uses `ObjectMapper.readValue` to parse into `HackerNewsItem` (internal static class). Logs raw JSON on error.
    - Includes timeout (`120s`) and exponential backoff retry.
    - Returns `Mono<HackerNewsItem>`. If fetching/parsing fails, returns a `Mono` containing a basic `HackerNewsItem` with only the ID set.
  - `HackerNewsItem` (Internal Static Class): DTO mapping the HN API item structure (`id`, `by`, `title`, `url`, `text`, `type`, `time`, `kids`, `score`, `descendants`). Uses `@JsonIgnoreProperties(ignoreUnknown = true)`.
- **`TranslationService.java`**:
  - Responsible for calling the DeepL translation API.
  - Uses `WebClient`. Reads API key and URL from properties (`@Value`).
  - `translateEnToZh(String text)`:
    - Checks for empty input.
    - Calls private `translateWithDeepL`.
    - Returns `Mono<String>`.
  - `translateWithDeepL(String text)`:
    - Truncates text longer than 5000 characters.
    - Builds JSON request body (`{"text": ["..."], "source_lang": "EN", "target_lang": "ZH"}`).
    - Sends POST request with `Authorization: DeepL-Auth-Key ...` header.
    - Parses the translation from the JSON response (`response.translations[0].text`).
    - Includes error handling and logging.
- **`CacheService.java`**:
  - Provides an interface for Redis caching operations. Uses Lombok `@RequiredArgsConstructor`.
  - Injects both `redisTemplate` (String, String) and `newsItemRedisTemplate` (String, NewsItem).
  - Defines prefixes (`translation:`, `news:`) and TTL (24 hours).
  - Methods:
    - `cacheTranslation(String key, String translation)` / `getCachedTranslation(String key)`: Uses `redisTemplate`.
    - `cacheNewsItem(Long id, NewsItem item)` / `getCachedNewsItem(Long id)`: Uses `newsItemRedisTemplate`.
    - `clearAllCache()`: Deletes keys matching defined prefixes.
- **`NewsUpdateService.java`**:
  - Orchestrates the main data fetching and processing workflow.
  - `updateNews()`: Triggered by `@Scheduled(fixedDelayString = "${hacker-news.poll.interval}")`.
    - Fetches top story IDs via `HackerNewsService`.
    - **Batch Processing:** Splits IDs into batches (`BATCH_SIZE=10`).
    - **Sequential Batch Execution with Delay:** Uses `Flux.concatMap` with `Mono.delay` between batches (`BATCH_DELAY_SECONDS=2`) to avoid overwhelming APIs.
    - **Concurrent Item Processing within Batch:** Uses `Flux.flatMap` with concurrency limit (`BATCH_CONCURRENCY=3`) to process items within a batch in parallel.
    - **Error Handling & Retry:** Uses `doOnError` to track failed IDs (`failedIds` Set) and `onErrorResume` to continue processing. After main run, calls `scheduleRetryFailedIds` if needed, which retries failed IDs with smaller batches and longer delays.
    - **Item Processing Logic (`processNewsItem` called internally):**
      - Checks DB existence (`repository.existsById`).
      - If exists: Updates `rank` and `score`, saves (`repository.save`), skips translation.
      - If not exists:
        - Checks translation cache (`cacheService.getCachedTranslation`).
        - If miss: Calls `translationService.translateEnToZh`.
        - If translation successful: Caches translation (`cacheService.cacheTranslation`).
        - Builds `NewsItem` entity.
        - Saves to DB (`repository.save`).
      - Caches the full `NewsItem` (`cacheService.cacheNewsItem`).
    - **Data Cleanup:** Calls `cleanupOldNews()` after processing.
  - `cleanupOldNews()`:
    - Checks if total news count exceeds `hackerNewsConfig.getMaxStoredItems()`.
    - If yes, finds all news sorted by `lastUpdated` DESC, then `rank` ASC.
    - Deletes items beyond the limit (`repository.deleteAllById`).

### 4.5 `controller` Package

- **`NewsController.java` (`/api/news`)**:
  - Standard `@RestController` providing JSON API for news.
  - `getLatestNews()` (GET `/`): Returns `List<NewsItem>` (top 50 by time desc). _Attempts_ to warm the `NewsItem` cache after fetching from DB.
  - `getNewsById(Long id)` (GET `/{id}`): Returns `Optional<NewsItem>`. Implements cache-aside pattern (check cache first, if miss, query DB, then populate cache).
  - `testTranslate(String text)` (GET `/test-translate`): Simple endpoint to test `TranslationService`.
- **`IndexController.java` (`/`)**:
  - **Server-Side Rendering (SSR) Controller** (`@RestController`, but produces `text/html`).
  - Handles requests to the root path (`/`).
  - **Directly generates the HTML homepage content** using `StringBuilder`.
  - Fetches news from `NewsItemRepository` (sorted by `rank` ASC, `lastUpdated` DESC), filters duplicates by rank, limits to 50.
  - Builds a full HTML page with inline CSS, header, status section (last update time, total items, refresh buttons linking to `/api/test/manual-update` and JS reload), news list (showing ZH title, EN title below if ZH exists, rank, score, type, time, ID), and footer.
  - Provides a fallback error HTML page.
  - **This conflicts/duplicates the functionality of `frontend/index.html` + `script.js`.**
- **`TestController.java` (`/api/test`)**:
  - **Extremely large utility controller** for debugging and testing (~1900 lines).
  - Contains numerous endpoints (`/db-test`, `/cache-test`, `/translate`, `/trigger-update`, `/create-test-data`, `/check-hn-api`, `/fetch-one`, `/dashboard`, `/fix-incomplete-data`, `/raw-api-data`, `/batch-fetch`, `/view-all-news`, `/complete-reset`, etc.).
  - Allows testing almost every component and workflow manually.
  - The `/dashboard` endpoint generates a very detailed HTML status page.
  - **Should be disabled/removed in production.**
- **`EnvTestController.java` (`/api/test/env`)**:
  - Specific test endpoint to verify environment variable loading via `Environment` and `@Value`. Checks `MYSQL_USERNAME`, `REDIS_HOST`, `BAIDU_TRANSLATE_APPID` (note: Baidu mentioned, but DeepL used in service).
- **`HomeController.java` (`/old_index`)**:
  - Uses `@Controller`.
  - Maps `/old_index` to return the content of `classpath:static/index.html`.
  - Purpose is unclear, as main static files seem served from `file:frontend/` via `WebFluxConfig`. Might be legacy or unused. `static/index.html` likely doesn't match `frontend/index.html`.

### 4.6 `util` Package

- **`SpringContextUtil.java`**:
  - Implements `ApplicationContextAware` to capture the Spring `ApplicationContext`.
  - Provides static methods (`getBean`) to retrieve Spring beans from non-managed classes or static contexts.
  - Use of this pattern should generally be avoided in favor of standard DI. Unclear if/where it's used in the project.

### 4.7 `dao` Package

- **`TranslationCache.java`**: File exists but is **empty**. Package likely unused.

### 4.8 `common` Package

- **`Result.java`**: File exists but is **empty**. Package likely unused.

## 5. Core Workflows

### 5.1 Application Startup

1.  `HackerCNewsApplication.main()` starts Spring Boot.
2.  Spring scans for components (`@Service`, `@Repository`, `@Controller`, `@Configuration`, etc.).
3.  Beans are instantiated.
4.  Dependencies are injected (mostly constructor injection).
5.  `PropertiesConfig` loads `.env.properties` into `Environment` and System Properties.
6.  `application.properties` is loaded (using placeholders resolved from Environment/System Properties).
7.  `@Scheduled` tasks are registered.
8.  Web server (Netty via WebFlux) starts.

### 5.2 Scheduled News Update (`NewsUpdateService.updateNews`)

1.  Triggered by `@Scheduled` timer (e.g., every 3 minutes).
2.  Fetch top story IDs from HN (`HackerNewsService.getTopStories`).
3.  Divide IDs into batches (e.g., 10 IDs/batch).
4.  For each batch (sequentially, with delay between batches):
    - For each ID in batch (concurrently, up to limit):
      - Fetch HN item details (`HackerNewsService.getItemById`).
      - Check if item exists in DB (`repository.existsById`).
      - **If exists:** Update `rank`, `score`, `lastUpdated`. Save to DB. Skip translation.
      - **If not exists:**
        - Check translation cache (`cacheService.getCachedTranslation`).
        - If miss: Translate title/text via `TranslationService.translateEnToZh` (calls DeepL). Cache result if successful.
        - Assemble `NewsItem` entity.
        - Save to DB (`repository.save`). Set `createdAt`.
      - Cache the full `NewsItem` object (`cacheService.cacheNewsItem`).
      - Track any failed IDs.
5.  If any IDs failed, schedule retries (`scheduleRetryFailedIds`) with smaller batches/longer delays.
6.  Clean up old news records exceeding the configured limit (`cleanupOldNews`).

### 5.3 User Request Handling

- **SSR Homepage (Accessing `/`)**:
  1.  Request hits `IndexController.index()`.
  2.  Controller queries DB via `NewsItemRepository` (finds all, sorts by rank/time, filters duplicates, limits).
  3.  Controller builds HTML string using `StringBuilder`.
  4.  HTML response sent to browser.
- **CSR Homepage (Accessing `/index.html` or similar)**:
  1.  Request hits `WebFluxConfig` resource handler (or potentially `HomeController` for `/old_index`).
  2.  `frontend/index.html`, `css/style.css`, `js/script.js` are served.
  3.  `script.js` executes `fetch('/api/news')`.
  4.  Request hits `NewsController.getLatestNews()`.
  5.  Controller queries DB (`repository.findTop50ByOrderByTimeDesc`).
  6.  Controller attempts to warm cache (`cacheService.cacheNewsItem`).
  7.  JSON `List<NewsItem>` sent back to `script.js`.
  8.  `script.js` dynamically renders the news list in the HTML DOM.
- **API Request for Single Item (e.g., `fetch('/api/news/123')`)**:
  1.  Request hits `NewsController.getNewsById(123)`.
  2.  Controller checks cache (`cacheService.getCachedNewsItem(123)`).
  3.  If hit: Return cached `NewsItem` as JSON.
  4.  If miss: Query DB (`repository.findById(123)`).
  5.  If DB hit: Cache the result (`cacheService.cacheNewsItem`). Return DB result as JSON.
  6.  If DB miss: Return empty optional/appropriate response.

## 6. Frontend (`frontend/` directory)

- Consists of basic `index.html`, `css/style.css`, `js/script.js`.
- Implements **Client-Side Rendering (CSR)**.
- `script.js`:
  - On `DOMContentLoaded`, fetches `/api/news`.
  - Displays a loading indicator.
  - Parses the JSON response (list of `NewsItem`).
  - Filters items without titles.
  - Dynamically creates `<li>` elements for each valid item and appends to `#news-list`.
  - Displays ZH title (or EN if ZH missing). Links to item `url` (or `/news/{id}` if no URL - this internal link is currently unhandled).
  - Displays meta info (type, formatted time).
  - Includes basic error handling.
- This CSR implementation is **parallel to/conflicts with** the SSR implementation in `IndexController`.

## 7. Key Findings & Potential Issues

- **Dual Homepage Implementations:** `IndexController` (SSR at `/`) and `frontend/` dir (CSR at `/index.html`) exist concurrently. Needs clarification/consolidation.
- **Extensive Test Controller (`TestController`):** Very useful for development but poses a significant security risk if exposed in production. Contains data manipulation and reset endpoints. Must be disabled/removed for deployment.
- **Translation Service Discrepancy:** Test controllers mention Baidu Translate, but `TranslationService` uses DeepL. Minor inconsistency, likely legacy code in tests.
- **Unclear `HomeController`:** The purpose of `/old_index` mapping to `static/index.html` is unclear given other static file serving mechanisms and the location of actual frontend files. Likely safe to remove.
- **Unused Code/Packages:** `util/SpringContextUtil` (usage uncertain), `dao/` package (empty), `common/` package (empty) might be removable cruft.
- **Missing Iteration Features:** No sign of User Auth (Spring Security), Community Features (Entities, Repos, Services, Controllers), Vue.js, Microservices, Messaging (RabbitMQ), or advanced AI (LangChain4j).

## 8. Current Status vs. ImplementationGuide.md

- **MVP:** Largely **achieved** regarding backend data pipeline (fetch, translate, store, cache, API). Frontend MVP also exists (in two forms). Robustness features (retry, batching, etc.) were implemented well.
- **Iteration 1:** **Not Started.** No user management, community features, Spring Security, or Vue.js integration.
- **Iteration 2:** **Not Started.** Still a monolith, no microservices, Spring Cloud, RabbitMQ, or LangChain4j.

This document provides a detailed snapshot of the codebase as analyzed. It should serve as a reliable reference for understanding the current implementation details.
