# Collins Dictionary API Developer Documentation

> Based on **Dictionary API Documentation v0.4** supplied by Collins.
>
> 本文在官方 API 文档基础上重新整理为 Markdown，并在最后增加"搜索解析与
> Wiki 式搜索体验实现方案"一节。官方文档明确建议出于安全原因，将 API
> 及其客户端库用于服务端应用，而不要直接用于客户端应用。

------------------------------------------------------------------------

## 1. API 概览

### 1.1 API 地址

API 网站：

-   `https://api.collinsdictionary.com`

官方文档还提供 API Demo：

-   `https://api.collinsdictionary.com/apidemo/`

使用 Demo 时需要提供自己的 access key。

### 1.2 API 特性

Collins Dictionary API：

-   是 RESTful API
-   支持 JSON 和 XML 两种返回格式
-   API 版本当前为 `v1`
-   每个用户使用唯一 access key
-   access key 决定可访问的数据集以及可用调用次数

官方文档列出了 Java、Perl、PHP、JavaScript、Objective-C
等客户端库/示例。

API 客户端库：

-   `http://dps.api-lib.idm.fr/`

> 注意：官方文档中的部分 URL 使用 `http://` 形式展示，但同时明确说明 API
> 仅支持 HTTPS。因此实际调用应使用 `https://`。

------------------------------------------------------------------------

# 2. Access Key

每个 API 用户拥有唯一的 access key。

官方文档说明：

> The key will determine access to specific datasets and the number of
> calls available.

也就是说，access key 不仅用于身份验证，还决定：

1.  可以访问哪些 dictionary dataset
2.  可以进行多少次 API 调用

文档中的示例 access key 为 64 字符文本。

## 2.1 请求头

API 示例使用：

``` http
accessKey: YOUR_ACCESS_KEY
```

例如：

``` http
GET /api/v1/dictionaries/british/entries/car?format=html HTTP/1.0
Host: api.collinsdictionary.com
Accept: application/json
accessKey: YOUR_ACCESS_KEY
```

------------------------------------------------------------------------

# 3. 返回格式

API 支持：

-   JSON
-   XML

返回格式由 HTTP `Accept` 请求头决定。

  `Accept`             返回结果
  -------------------- ----------
  `application/json`   JSON
  `application/xml`    XML
  未提供               JSON
  其他值               HTTP 406

因此，如果你的应用只使用 JSON，可以统一发送：

``` http
Accept: application/json
```

------------------------------------------------------------------------

# 4. API URL 结构

统一格式：

``` text
https://<hostname>/api/v1/...
```

其中：

  部分                                  含义
  ------------------------------------- ------------------
  `https://api.collinsdictionary.com`   Collins API 主机
  `api`                                 API 请求标识
  `v1`                                  API 版本
  `...`                                 具体 API 方法

官方文档说明，未来版本可能使用：

``` text
v2
v3
...
```

因此客户端最好不要把版本号散落在代码中，而应该集中配置。

例如：

``` kotlin
const val API_BASE_URL =
    "https://api.collinsdictionary.com/api/v1/"
```

------------------------------------------------------------------------

# 5. API 方法

## 5.1 获取可用词典列表

### Endpoint

``` http
GET /api/v1/dictionaries
```

### 参数

无。

### JSON 返回

返回一个 dictionary 数组。

每个 dictionary 包含：

``` json
{
  "dictionaryCode": "british",
  "dictionaryName": "British English",
  "dictionaryUrl": "..."
}
```

字段：

  字段               含义
  ------------------ ----------------------------
  `dictionaryCode`   词典代码
  `dictionaryName`   面向用户显示的词典名称
  `dictionaryUrl`    主站对应的词典浏览列表 URL

如果没有可用词典，返回空数组。

### XML

结构：

``` xml
<dictionaries>
    <dictionary>
        <dictionaryCode>{dictionaryCode}</dictionaryCode>
        <dictionaryName>{dictionaryName}</dictionaryName>
        <dictionaryUrl>{dictionaryURL}</dictionaryUrl>
    </dictionary>
</dictionaries>
```

如果没有词典：

``` xml
<dictionaries />
```

------------------------------------------------------------------------

# 6. 获取指定词典

## Endpoint

``` http
GET /api/v1/dictionaries/{dictionaryCode}
```

### 参数

  参数                 必填 说明
  ------------------ ------ ----------
  `dictionaryCode`       是 词典代码

例如：

``` text
/api/v1/dictionaries/british
```

### JSON

``` json
{
  "dictionaryCode": "british",
  "dictionaryName": "British English",
  "dictionaryUrl": "..."
}
```

### 字段

  字段               含义
  ------------------ --------------------
  `dictionaryCode`   词典代码
  `dictionaryName`   用户友好的词典名称
  `dictionaryUrl`    词典浏览列表 URL

如果 dictionary 不存在，API 返回错误。

------------------------------------------------------------------------

# 7. 搜索

这是整个 Dictionary API 中最重要的方法之一。

## Endpoint

``` http
GET /api/v1/dictionaries/{dictionaryCode}/search/?q={searchWord}&pagesize={pageSize}&pageindex={pageIndex}
```

例如：

``` text
https://api.collinsdictionary.com/api/v1/dictionaries/british/search/?q=apple&pagesize=10&pageindex=1
```

## 参数

  参数                          必填   默认值 限制
  --------------------------- ------ -------- ---------------
  `dictionaryCode`                是       \- 词典代码
  `q` / `searchWord`              是       \- 要搜索的单词
  `pagesize` / `pageSize`         否       10 最大 100
  `pageindex` / `pageIndex`       否        1 从第 1 页开始

## JSON 返回

主要结构：

``` json
{
  "dictionaryCode": "british",
  "resultNumber": 10,
  "pageNumber": 1,
  "currentPageIndex": 1,
  "results": [
    {
      "entryId": "apple",
      "entryLabel": "apple",
      "entryUrl": "..."
    }
  ]
}
```

### 字段

  字段                 含义
  -------------------- ----------------------
  `dictionaryCode`     词典代码
  `resultNumber`       搜索结果总数
  `pageNumber`         最后一个结果页的页码
  `currentPageIndex`   当前结果页
  `results`            搜索结果数组

每个 result：

  字段           含义
  -------------- ---------------------------------
  `entryId`      entry ID
  `entryLabel`   entry 的 headword
  `entryUrl`     Collins 主站对应 entry 页面 URL

官方文档指出，结果按照主词典中的相同顺序排列。

没有搜索结果时：

``` json
{
  "results": []
}
```

如果 dictionary 不存在，则返回错误。

------------------------------------------------------------------------

# 8. Did You Mean

用于拼写纠错和搜索建议。

## Endpoint

``` http
GET /api/v1/dictionaries/{dictionaryCode}/search/didyoumean/?q={searchWord}&entrynumber={entryNumber}
```

例如：

``` text
/api/v1/dictionaries/british/search/didyoumean/?q=appple&entrynumber=3
```

## 参数

  参数                              必填   默认值   最大值
  ------------------------------- ------ -------- --------
  `dictionaryCode`                    是       \-       \-
  `q` / `searchWord`                  是       \-       \-
  `entrynumber` / `entryNumber`       否       10       10

## JSON

``` json
{
  "dictionaryCode": "british",
  "searchTerm": "appple",
  "suggestions": [
    "apple"
  ]
}
```

字段：

  字段               含义
  ------------------ -----------------------
  `dictionaryCode`   词典代码
  `searchTerm`       原始搜索词
  `suggestions`      Did You Mean 建议列表

每一个 suggestion：

``` json
{
  "suggestion": "apple"
}
```

没有建议时：

``` json
{
  "suggestions": []
}
```

------------------------------------------------------------------------

# 9. 获取第一/最佳匹配 Entry

## Endpoint

``` http
GET /api/v1/dictionaries/{dictionaryCode}/search/first/?q={searchWord}&format={format}
```

这是一个非常有用的 API。

它用于获取指定搜索词对应的第一/最佳匹配 entry。

## 参数

  参数                   必填   默认值
  -------------------- ------ --------
  `dictionaryCode`         是       \-
  `q` / `searchWord`       是       \-
  `format`                 否   `html`

`format` 可以：

``` text
html
xml
```

## JSON 返回

``` json
{
  "dictionaryCode": "british",
  "format": "html",
  "entryContent": "...",
  "entryId": "apple",
  "entryLabel": "apple",
  "entryUrl": "...",
  "topics": []
}
```

### Entry 字段

  字段               含义
  ------------------ -----------------
  `dictionaryCode`   dictionary code
  `format`           entry 内容格式
  `entryContent`     entry 内容
  `entryId`          entry ID
  `entryLabel`       entry headword
  `entryUrl`         主站 entry URL
  `topics`           关联 topics

### Topic

每个 topic：

  字段              含义
  ----------------- -------------
  `topicId`         topic ID
  `topicLabel`      topic 名称
  `topicUrl`        topic URL
  `topicParentId`   父 topic ID

如果 dictionary 不存在或者没有搜索结果，返回错误。

------------------------------------------------------------------------

# 10. 获取指定 Entry

## Endpoint

``` http
GET /api/v1/dictionaries/{dictionaryCode}/entries/{entryId}?format={format}
```

例如：

``` text
/api/v1/dictionaries/british/entries/apple?format=html
```

## 参数

  参数                 必填   默认值
  ------------------ ------ --------
  `dictionaryCode`       是       \-
  `entryId`              是       \-
  `format`               否   `html`

`format`：

``` text
html
xml
```

## JSON

``` json
{
  "dictionaryCode": "british",
  "format": "html",
  "entryContent": "...",
  "entryId": "apple",
  "entryLabel": "apple",
  "entryUrl": "...",
  "topics": []
}
```

与 `/search/first` 返回的 entry 数据结构基本一致。

如果没有结果，官方文档说明返回：

``` json
{}
```

如果 dictionary 或 entry 不存在，则返回错误。

API 会对 entry 内容进行适当
escaping，以保证其在指定返回格式中构成合法字符串。

------------------------------------------------------------------------

# 11. 获取发音

## Endpoint

``` http
GET /api/v1/dictionaries/{dictionaryCode}/entries/{entryId}/pronunciations?lang={lang}
```

## 参数

  参数                 必填             默认值
  ------------------ ------ ------------------
  `dictionaryCode`       是                 \-
  `entryId`              是                 \-
  `lang`                 否   所有可用发音语言

## JSON

返回 pronunciation 数组。

每个 pronunciation：

``` json
{
  "dictionaryCode": "british",
  "entryId": "apple",
  "lang": "en",
  "pronunciationUrl": "..."
}
```

字段：

  字段                 含义
  -------------------- -----------------
  `dictionaryCode`     dictionary code
  `entryId`            entry ID
  `lang`               发音语言
  `pronunciationUrl`   MP3 文件 URL

没有结果时，官方文档说明返回空 JSON 对象。

XML 则返回：

``` xml
<pronunciations>
    <pronunciation>
        <dictionaryCode>{dictionaryCode}</dictionaryCode>
        <entryId>{entryId}</entryId>
        <lang>{lang}</lang>
        <url>{pronunciationUrl}</url>
    </pronunciation>
</pronunciations>
```

------------------------------------------------------------------------

# 12. 获取附近 Entry

这个接口适合实现类似传统纸质词典的"上一个词 / 下一个词"。

## Endpoint

``` http
GET /api/v1/dictionaries/{dictionaryCode}/entries/{entryId}/nearbyentries?entrynumber={entryNumber}
```

## 参数

  参数                              必填   默认值   最大值
  ------------------------------- ------ -------- --------
  `dictionaryCode`                    是       \-       \-
  `entryId`                           是       \-       \-
  `entrynumber` / `entryNumber`       否       10       50

`entryNumber` 表示给定 entry 前后各需要多少个附近结果。

## JSON

``` json
{
  "dictionaryCode": "british",
  "entryId": "apple",
  "nearbyFollowingEntries": [
    {
      "entryId": "...",
      "entryLabel": "...",
      "entryUrl": "..."
    }
  ],
  "nearbyPrecedingEntries": [
    {
      "entryId": "...",
      "entryLabel": "...",
      "entryUrl": "..."
    }
  ]
}
```

每个 nearby entry：

  字段           含义
  -------------- -----------
  `entryId`      entry ID
  `entryLabel`   headword
  `entryUrl`     entry URL

如果没有结果，相关数组为空。

如果 dictionary 或 entry 不存在，则返回错误。

> 官方文档中 `nearbyFollowingEntries` / `nearbyPrecedingEntries`
> 的文字说明存在 preceding/following
> 表述与字段命名的轻微混用。实现时建议以实际 API
> 返回的数据和字段名称为准，而不要自行交换两个数组。

------------------------------------------------------------------------

# 13. 错误处理

如果 API 方法产生 exception，HTTP 状态码为 `5xx`。

## JSON

``` json
{
  "errorCode": "...",
  "errorMessage": "..."
}
```

## XML

``` xml
<error>
    <code>{errorCode}</code>
    <message>{errorMessage}</message>
</error>
```

官方文档表示完整 error code 列表在在线文档中提供。

------------------------------------------------------------------------

# 14. 官方 API Client Libraries

官方文档列出了以下客户端库：

-   Java
-   C#
-   Objective-C
-   Perl
-   PHP
-   Ruby
-   Python
-   JavaScript

官方库：

``` text
http://dps.api-lib.idm.fr/libraries.html#js
```

这些库被官方描述为 proof-of-concept，用于演示如何构建简单的 API 接口。

------------------------------------------------------------------------

# 15. 安全要求

这是使用 Collins API 时非常重要的一点。

官方文档明确建议：

> 出于安全原因，API 和这些客户端库应该只用于 server-side
> applications，并且不应该用于 client-side applications。

因此：

``` text
Android / iOS / Desktop App
          │
          ▼
      自己的 API
          │
          ▼
   Collins Dictionary API
```

比：

``` text
Android / iOS / Desktop App
          │
          ▼
   Collins Dictionary API
```

更符合官方建议。

------------------------------------------------------------------------

# 17. Wiki 式搜索体验

## 17.1 目标

希望搜索框实现这样的行为：

``` text
输入一个确切的词条
        ↓
直接打开词条

输入不是确切词条
        ↓
展示搜索结果

搜索没有结果
        ↓
Did You Mean
```

例如：

``` text
apple
```

直接：

``` text
Apple dictionary entry
```

而：

``` text
appl
```

展示：

``` text
Search results

apple
application
applicable
...
```

而：

``` text
appple
```

展示：

``` text
Did you mean:
apple
```

------------------------------------------------------------------------

# 18. 不要把 `/search/first` 直接当成 Exact Match

Collins API 提供：

``` text
/search/first
```

它的语义是：

> Get the first/best matching entry

这与：

> 用户输入是否恰好对应一个词条

并不是完全相同的概念。

例如：

``` text
appl
```

如果 `/search/first` 返回：

``` text
apple
```

并不意味着 `appl` 就是一个 exact entry。

因此应该区分：

``` text
Exact Match
Best Match
Search Result
Did You Mean
```

------------------------------------------------------------------------

# 19. 推荐的 Search Resolution Pipeline

推荐使用以下优先级：

``` text
                         User Query
                              │
                              ▼
                       Normalize Query
                              │
                              ▼
                    ┌──────────────────┐
                    │ Exact Match?     │
                    └────────┬─────────┘
                             │
                    ┌────────┴────────┐
                    │                 │
                   YES                NO
                    │                 │
                    ▼                 ▼
              Open Entry         Collins Search
                                      │
                             ┌────────┴────────┐
                             │                 │
                          Results             Empty
                             │                 │
                             ▼                 ▼
                        Result List       Did You Mean
                                               │
                                               ▼
                                          Suggestions
```

------------------------------------------------------------------------

# 20. 第一层：Query Normalization

在查询 API 之前，先统一用户输入。

最基础的 normalization：

``` kotlin
fun normalizeQuery(query: String): String {
    return query
        .trim()
        .lowercase()
}
```

例如：

``` text
" Apple "
      ↓
"apple"

"APPLE"
      ↓
"apple"
```

更完善的实现可以进一步考虑：

-   Unicode normalization
-   连续空白
-   Unicode apostrophe
-   连字符
-   大小写
-   重音符号

但是不要在没有验证 Collins 数据规则之前过度修改用户输入。

------------------------------------------------------------------------

# 21. 第二层：Exact Match

核心思想：

``` text
normalizedQuery
       │
       ▼
Exact Index
       │
       ▼
Entry ID
```

可以维护：

``` text
normalized_headword → entryId
```

例如：

``` json
{
  "apple": "apple",
  "application": "application",
  "look up": "look-up"
}
```

数据库实现可以是：

``` sql
SELECT *
FROM entries
WHERE normalized_headword = ?
LIMIT 1;
```

如果命中：

``` text
SearchResult.Entry
```

直接进入 entry 页面。

------------------------------------------------------------------------

# 22. Exact Match 不应该只考虑单个单词

Dictionary entry 可能包含：

``` text
apple
look up
take off
in spite of
```

所以索引对象应该是：

``` text
Entry
```

而不是：

``` text
Word
```

例如：

``` text
"look up"
    ↓
phrasal verb entry
```

也应该可以 Exact Match。

------------------------------------------------------------------------

# 24. 第三层：Collins Search

Exact Match 失败后调用：

``` http
GET /api/v1/dictionaries/{dictionaryCode}/search/?q={searchWord}&pagesize=...
```

例如：

``` text
appl
```

得到若干搜索结果：

``` text
apple
application
applicable
appliance
...
```

UI：

``` text
Search Results

apple
application
applicable
appliance
```

Collins API 已经负责返回搜索结果以及其排序。

因此客户端没有必要一开始就自己实现复杂的全文检索算法。

------------------------------------------------------------------------

# 25. 第四层：Did You Mean

只有搜索结果为空，或者你的产品策略认为搜索结果质量过低时，再请求：

``` http
GET /api/v1/dictionaries/{dictionaryCode}/search/didyoumean/?q={searchWord}
```

例如：

``` text
appple
```

得到：

``` text
apple
```

UI：

``` text
No results for "appple"

Did you mean "apple"?
```

点击：

``` text
apple
```

重新走完整的 Search Resolution Pipeline。

------------------------------------------------------------------------

# 26. 推荐的 Kotlin 数据模型

如果客户端使用 Kotlin，可以定义：

``` kotlin
sealed interface SearchResult {

    data class ExactEntry(
        val entryId: String,
        val entryLabel: String
    ) : SearchResult

    data class Results(
        val items: List<SearchItem>,
        val total: Int
    ) : SearchResult

    data class DidYouMean(
        val query: String,
        val suggestions: List<String>
    ) : SearchResult

    data object NotFound : SearchResult

    data class Error(
        val message: String
    ) : SearchResult
}
```

搜索结果：

``` kotlin
data class SearchItem(
    val entryId: String,
    val entryLabel: String,
    val entryUrl: String
)
```

------------------------------------------------------------------------

# 27. 推荐的 SearchResolver

核心逻辑可以设计成：

``` kotlin
class SearchResolver(
    private val exactIndex: ExactIndex,
    private val collinsApi: CollinsApi
) {

    suspend fun resolve(query: String): SearchResult {
        val normalized = normalizeQuery(query)

        exactIndex.find(normalized)?.let { entry ->
            return SearchResult.ExactEntry(
                entryId = entry.entryId,
                entryLabel = entry.label
            )
        }

        val results = collinsApi.search(
            query = normalized,
            pageSize = 20,
            pageIndex = 1
        )

        if (results.results.isNotEmpty()) {
            return SearchResult.Results(
                items = results.results,
                total = results.resultNumber
            )
        }

        val suggestions = collinsApi.didYouMean(
            query = normalized,
            entryNumber = 5
        )

        if (suggestions.isNotEmpty()) {
            return SearchResult.DidYouMean(
                query = query,
                suggestions = suggestions
            )
        }

        return SearchResult.NotFound
    }
}
```

------------------------------------------------------------------------

# 28. 关于 `/search/first` 的合理用法

`/search/first` 不建议放在：

``` text
Exact Match
```

这一层。

它更适合：

``` text
Search → Best Match
```

例如：

``` text
用户搜索 appl
       ↓
Search
       ↓
Best Match
       ↓
apple
```

如果产品希望：

> 输入后按 Enter 直接进入最相关的结果

那么可以使用：

``` text
/search/first
```

实现。

但这与：

> 用户输入的是不是一个真实存在的 headword

是两种不同需求。

------------------------------------------------------------------------

# 29. API 调用策略

推荐：

``` text
Exact Match
    │
    ├── hit → 0 次 Collins Search API
    │
    └── miss
          │
          ▼
       Search
          │
          ├── results → 返回
          │
          └── empty
                │
                ▼
           Did You Mean
```

因此：

### Exact 命中

``` text
本地索引
→ 直接打开
```

不需要调用 Collins Search。

### 普通搜索

``` text
/search
```

### 无结果

``` text
/search
    ↓
/search/didyoumean
```

------------------------------------------------------------------------

# 32. UI 状态

搜索 UI 可以定义：

``` kotlin
sealed interface SearchUiState {

    data object Idle : SearchUiState

    data object Loading : SearchUiState

    data class Entry(
        val entryId: String
    ) : SearchUiState

    data class Results(
        val items: List<SearchItem>,
        val total: Int
    ) : SearchUiState

    data class DidYouMean(
        val query: String,
        val suggestions: List<String>
    ) : SearchUiState

    data class Empty(
        val query: String
    ) : SearchUiState

    data class Error(
        val message: String
    ) : SearchUiState
}
```

这样 UI 不需要知道：

``` text
到底调用了几个 API
```

UI 只负责：

``` text
SearchUiState → Render
```

------------------------------------------------------------------------

# 33. 推荐的用户体验

最终可以实现：

## 精确查询

``` text
┌──────────────────────────┐
│ apple                 🔍 │
└──────────────────────────┘

apple
/əˈpəl/

noun

...
```

直接进入 entry。

## 模糊查询

``` text
┌──────────────────────────┐
│ appl                   🔍 │
└──────────────────────────┘

Search results

apple
application
applicable
appliance
```

## 拼写错误

``` text
┌──────────────────────────┐
│ appple                 🔍 │
└──────────────────────────┘

No exact results.

Did you mean "apple"?
```

------------------------------------------------------------------------

# 34. 与 Wikipedia 式体验的对应关系

可以把整个系统理解为：

  用户行为     处理层          Collins API
  ------------ --------------- -----------------------------
  完全匹配     Exact Index     不一定需要 API
  普通搜索     Search          `/search`
  最佳匹配     Best Match      `/search/first`
  拼写纠正     Did You Mean    `/search/didyoumean`
  打开词条     Entry           `/entries/{entryId}`
  发音         Pronunciation   `/pronunciations`
  前后词条     Nearby          `/nearbyentries`

------------------------------------------------------------------------

# 35. 最终推荐流程

如果你的目标就是做一个具有 Wikipedia 式搜索体验的 Collins Dictionary
客户端，我建议最终采用：

``` text
                         ┌──────────────┐
                         │ User Query   │
                         └──────┬───────┘
                                │
                                ▼
                       ┌────────────────┐
                       │ Normalize      │
                       └───────┬────────┘
                               │
                               ▼
                    ┌──────────────────────┐
                    │ Local Exact Index    │
                    └──────────┬───────────┘
                               │
                     ┌─────────┴─────────┐
                     │                   │
                   FOUND               NOT FOUND
                     │                   │
                     ▼                   ▼
               ┌──────────┐       ┌─────────────┐
               │ Entry    │       │ /search     │
               │ Page     │       └──────┬──────┘
               └──────────┘              │
                                  ┌──────┴──────┐
                                  │             │
                               RESULTS        EMPTY
                                  │             │
                                  ▼             ▼
                            Result List   /didyoumean
                                                │
                                                ▼
                                          Suggestions
```

核心原则只有一句话：

> **Exact Match 决定"是不是直接进入词条"，Search
> 决定"有哪些相关结果"，Did You Mean 决定"用户是不是拼错了"。**

而 Collins v0.4 恰好提供了这套流程所需要的主要
API：`search`、`search/first`、`search/didyoumean`、`entries`、`pronunciations`
和 `nearbyentries`。
