# RongQi Vector

RongQi Vector 是一个 Java 17+ 通用向量搜索框架，用来把“文本转向量、写入 Milvus、创建 Collection、创建索引、向量检索、条件删除”这些重复工作封装起来。

它支持两种接入方式：

- **Maven 注解模式**：Java / Spring Boot 项目引入依赖，在自己的 domain 类上加注解，直接调用 `VectorTemplate`。
- **独立 HTTP 服务模式**：不会 Java 开发也可以通过 HTTP 接口创建 Collection、定义字段、定义索引、写入数据、搜索数据、删除数据。

## 背景

很多业务系统以后都会用到向量搜索，例如知识库、教材检索、商品搜索、问答召回、相似内容推荐。如果每个系统都自己写一遍 Milvus 连接、Collection 创建、字段定义、索引创建、Embedding 调用和过滤条件拼接，代码会重复、容易出错，也不方便统一维护。

RongQi Vector 的目标是：

- **简单配置**：不需要每个业务系统都理解 Milvus SDK 细节。
- **易用好用**：Java 用户用注解和对象调用；非 Java 用户用 HTTP 请求调用。
- **灵活扩展**：字段、Collection、索引、Embedding 模型都可以自定义。
- **统一规范**：后续业务系统接入向量搜索时，调用方式、错误格式、开发规范保持一致。

## 常见专业名词说明

如果你第一次接触向量检索，可以先看这一节。下面这些词会在配置、注解、HTTP 请求和源码中反复出现。

| 名词 | 简单理解 | 在本项目中的例子 |
| --- | --- | --- |
| `Vector` / 向量 | 一串数字，用来表示一段文本的语义。意思相近的文本，向量距离通常更近。 | `[0.12, -0.03, 0.98, ...]` |
| `Embedding` / 文本向量化 | 把文本转换成向量的过程。比如把“发票怎么申请”转换成一组数字。 | `EmbeddingProvider.embed(texts, options)` |
| `EmbeddingProvider` | 真正负责调用向量模型的组件。可以是 HTTP 模型服务，也可以是你自己实现的本地模型。 | `http`、`noop`、`local-bge` |
| `Model` / 模型 | 生成向量的具体模型名称。不同模型输出维度可能不同。 | `text-embedding-v4` |
| `Dimension` / 向量维度 | 每条向量有多少个数字。模型输出 1024 个数字，字段维度就必须配置成 1024。 | `dimension: 1024` |
| `Milvus` | 专门存储和检索向量的数据库。业务数据和向量最终会写入 Milvus。 | `http://127.0.0.1:19530` |
| `Collection` | Milvus 中的一张“表”。同一类业务数据通常放在同一个 Collection 里。 | `knowledge_chunk_v1` |
| `Schema` | Collection 的结构定义，说明有哪些字段、字段类型是什么、哪个字段是向量字段。 | `fields`、`indexes`、`embeddings` |
| `Field` / 字段 | Collection 中的一列数据。可以是主键、标题、正文、向量等。 | `chunk_id`、`title`、`content`、`embedding` |
| `PrimaryKey` / 主键 | 每条数据的唯一标识，用来删除、更新或区分不同数据。 | `chunk_id` |
| `AutoId` | 主键是否由 Milvus 自动生成。开启后写入时通常不需要手动传主键。 | `autoId: true` |
| `Index` / 索引 | 给向量字段建立的检索结构。没有索引时，大数据量搜索会很慢。 | `HNSW`、`AUTOINDEX` |
| `MetricType` / 距离算法 | 判断两个向量是否相似的计算方式。不同模型和业务可能使用不同算法。 | `COSINE`、`IP`、`L2` |
| `TopK` | 搜索时最多返回多少条最相似的数据。 | `topK: 10` |
| `CandidateTopK` | 第一阶段先从 Milvus 召回多少条候选结果，通常用于给二次排序更多候选。 | `candidateTopK: 50` |
| `Score` / 相似度分数 | 搜索结果的相似程度。通常用于排序或过滤低质量结果。 | `minScore: 0.7` |
| `Rank` / 二次排序 | 在向量相似度之外，再叠加业务字段权重调整结果顺序。 | `priority * 0.2` |
| `RankScore` | 二次排序后的分数。`score` 仍然保留 Milvus 原始相似度分数。 | `rankScore: 2.8` |
| `Filter` / 过滤条件 | 在向量搜索外，再按字段筛选数据。比如只查某个租户、某个业务类型。 | `tenant_id == 1001` |
| `LIKE` / 模糊匹配 | 字符串包含匹配。本项目会自动帮你补 `%`，用户只传关键词即可。 | 传入 `发票` 会变成 `%发票%` |
| `OutputFields` | 搜索结果需要返回哪些字段。只返回必要字段可以减少响应体大小。 | `chunk_id`、`title`、`content` |
| `Upsert` | 写入或更新数据。主键已存在时通常表示更新，不存在时表示新增。 | `/api/vector/documents/upsert` |
| `BatchSize` | 批量处理时每批多少条数据。比如每 32 条文本调用一次 Embedding 服务。 | `batch-size: 32` |
| `Timeout` | 请求外部服务最多等待多久。超过时间还没响应就认为失败。 | `timeout-millis: 30000` |
| `Retry` / 重试 | 遇到临时网络错误或服务 5xx 时自动再试几次，提高稳定性。 | `max-retries: 2` |
| `Token` / 密钥 | 访问 Milvus 或 Embedding 服务需要的鉴权信息。生产环境不要写死在代码里。 | `api-key`、`token` |
| `DynamicField` / 动态字段 | Milvus 是否允许写入 schema 里没有提前定义的字段。通常建议先关闭，避免脏数据。 | `dynamicFieldEnabled: false` |

## 整体架构

```text
业务系统
  |
  |-- 方式一：Maven 引入
  |      自定义 Domain + RongQi 注解
  |      调用 VectorTemplate
  |
  |-- 方式二：HTTP 调用
         调用 /api/vector/collections/ensure 定义 Collection
         调用 /api/vector/documents/upsert 写入数据
         调用 /api/vector/search 搜索数据
         调用 /api/vector/documents/delete 删除数据

RongQi Vector
  |
  |-- vector-annotation：注解和枚举
  |-- vector-core：核心接口、结果对象、异常、schema 定义
  |-- vector-embedding：Embedding 统一接口和 Provider 注册
  |-- vector-milvus：Milvus 创建、写入、检索、删除实现
  |-- vector-spring-boot-starter：Spring Boot 自动配置
  |-- vector-server：独立 HTTP 服务
  |-- vector-examples：示例代码
  |
  |-- Embedding 模型：DashScope / OpenAI 兼容接口 / Ollama / 本地模型 / 自定义模型
  |
Milvus
```

## 两种接入方式怎么选

| 接入方式 | 适合谁 | 怎么定义 Collection | 怎么调用 | 优点 |
| --- | --- | --- | --- | --- |
| Maven 注解模式 | Java / Spring Boot 项目 | 在业务 domain 类上加 `@VectorCollection`、`@VectorField`、`@VectorIndex` 等注解 | 直接注入 `VectorTemplate` 调用 | 类型安全、代码内聚、适合长期维护 |
| 独立 HTTP 服务模式 | 非 Java 系统、脚本、测试工具、低代码平台 | 调用 `/api/vector/collections/ensure` 传入字段、索引、Embedding 映射 | 通过 HTTP 调用写入、搜索、删除接口 | 不要求调用方会 Java，不需要重新发版业务系统 |

注意：**Collection 不写在 Spring Boot 配置文件里**。  
Maven 模式通过 domain 注解定义；HTTP 模式通过 `/api/vector/collections/ensure` 定义。这样别人导入 Maven 或使用 HTTP 服务时，都可以按自己的业务字段动态接入。

## 环境要求

| 项目 | 要求 |
| --- | --- |
| JDK | 17 或以上 |
| Spring Boot | 3.x |
| Maven | 3.8+ |
| Milvus | 2.x |
| Embedding 服务 | 支持 OpenAI 兼容 `/embeddings` 格式，或实现自定义 `EmbeddingProvider` |

## 项目模块

| 模块 | 说明 |
| --- | --- |
| `vector-annotation` | 对外注解和枚举，例如 Collection、字段、索引、Embedding 文本字段 |
| `vector-core` | 核心接口、结果对象、异常、domain 元数据解析、HTTP schema 定义 |
| `vector-embedding` | Embedding SPI、Provider 注册表、HTTP EmbeddingProvider |
| `vector-milvus` | Milvus Collection 创建、索引创建、写入、搜索、删除 |
| `vector-spring-boot-starter` | Spring Boot 自动配置，自动注入 `VectorTemplate` |
| `vector-server` | 独立 HTTP 服务，提供 `/api/vector/**` 接口 |
| `vector-examples` | 示例 domain 和调用代码 |

## 方式一：Maven 注解模式

这种方式适合 Java / Spring Boot 项目。业务项目只需要定义自己的 domain，然后调用 `VectorTemplate`。

### 1. 安装或引入依赖

如果 RongQi Vector 还没有发布到公司 Maven 私服，先在 RongQi Vector 项目根目录执行：

```powershell
mvn clean install
```

业务项目引入 starter：

```xml
<dependency>
    <groupId>com.rongqi.vector</groupId>
    <artifactId>vector-spring-boot-starter</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

推荐同时引入 Lombok，getter/setter 不需要手写：

```xml
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <version>1.18.34</version>
    <scope>provided</scope>
</dependency>
```

### 2. 配置 Milvus 和 Embedding

```yaml
rongqi:
  vector:
    milvus:
      uri: http://127.0.0.1:19530
      token:
      database: default
    embedding:
      default-provider: http
      batch-size: 32
    http-embedding:
      enabled: true
      name: http
      url: https://dashscope.aliyuncs.com/compatible-mode/v1
      api-key: ${DASHSCOPE_API_KEY}
      model: text-embedding-v4
      dimension: 1024
      timeout-millis: 30000
      max-retries: 2
      retry-interval-millis: 500
    schema:
      type: file
      storage-dir: data/rongqi-vector/collections
```

| 配置项 | 必填 | 说明 | 示例 |
| --- | --- | --- | --- |
| `rongqi.vector.milvus.uri` | 是 | Milvus 地址 | `http://127.0.0.1:19530` |
| `rongqi.vector.milvus.token` | 否 | Milvus token，没有鉴权可为空 | `<your-milvus-token>` |
| `rongqi.vector.milvus.database` | 否 | 默认 database，domain 注解没写 database 时使用 | `default` |
| `rongqi.vector.embedding.default-provider` | 是 | 默认 EmbeddingProvider 名称 | `http` |
| `rongqi.vector.embedding.batch-size` | 否 | 批量写入时每次调用 EmbeddingProvider 的文本数量，非法值会回退为 32 | `32` |
| `rongqi.vector.http-embedding.enabled` | 否 | 是否启用内置 HTTP EmbeddingProvider | `true` |
| `rongqi.vector.http-embedding.name` | 否 | 内置 HTTP Provider 名称 | `http` |
| `rongqi.vector.http-embedding.url` | 使用 HTTP Provider 时必填 | Embedding 服务地址，OpenAI 兼容 base url 会自动补 `/embeddings` | `https://dashscope.aliyuncs.com/compatible-mode/v1` |
| `rongqi.vector.http-embedding.api-key` | 视模型而定 | Embedding 服务密钥，建议用环境变量 | `${DASHSCOPE_API_KEY}` |
| `rongqi.vector.http-embedding.model` | 使用 HTTP Provider 时必填 | Embedding 模型名称 | `text-embedding-v4` |
| `rongqi.vector.http-embedding.dimension` | 是 | 模型输出向量维度，必须和向量字段维度一致 | `1024` |
| `rongqi.vector.http-embedding.max-retries` | 否 | HTTP Embedding 请求遇到 429、5xx 或网络异常时的最大重试次数 | `2` |
| `rongqi.vector.http-embedding.retry-interval-millis` | 否 | HTTP Embedding 每次重试前等待的毫秒数 | `500` |
| `rongqi.vector.schema.type` | 否 | HTTP Collection schema 存储方式，支持 `file`、`jdbc` | `file` |
| `rongqi.vector.schema.storage-dir` | HTTP 模式需要 | HTTP Collection schema 保存目录 | `data/rongqi-vector/collections` |
| `rongqi.vector.schema.jdbc.table-name` | JDBC 模式需要 | JDBC schema 表名，只能包含字母、数字、下划线 | `vector_collection_schema` |
| `rongqi.vector.schema.jdbc.initialize-schema` | 否 | JDBC 模式启动时是否自动建表 | `true` |

schema 存储只保存 HTTP collection 模式的 schema 定义，不保存向量数据；真正的向量和业务字段值存储在 Milvus 中。`file` 模式适合开发、单机测试和容器挂载卷；多实例生产环境建议使用 `jdbc` 模式，让多个服务实例共享同一张 schema 表。

JDBC schema 存储示例：

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/rongqi_vector
    username: root
    password: your-password

rongqi:
  vector:
    schema:
      type: jdbc
      jdbc:
        table-name: vector_collection_schema
        initialize-schema: true
```

JDBC 模式会使用 Spring 容器中的 `DataSource`。如果你的项目没有配置数据源，启动时会提示 `rongqi.vector.schema.type=jdbc 时必须提供 DataSource`。

### 3. 定义业务 Domain

下面是一个知识片段示例。重点看注解，不需要手写 getter/setter。

```java
package com.example.demo.domain;

import com.rongqi.vector.annotation.IndexType;
import com.rongqi.vector.annotation.MetricType;
import com.rongqi.vector.annotation.VectorCollection;
import com.rongqi.vector.annotation.VectorDataType;
import com.rongqi.vector.annotation.VectorEmbeddingText;
import com.rongqi.vector.annotation.VectorField;
import com.rongqi.vector.annotation.VectorId;
import com.rongqi.vector.annotation.VectorIndex;
import com.rongqi.vector.annotation.VectorIndexes;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 知识片段 domain。
 * Lombok 会自动生成 getter/setter，业务代码不需要手写。
 */
@Getter
@Setter
@NoArgsConstructor
@VectorCollection(
        name = "knowledge_chunk_v1",
        database = "knowledge_base",
        description = "知识片段向量集合"
)
@VectorIndexes({
        @VectorIndex(field = "embedding", name = "idx_embedding", type = IndexType.HNSW,
                metricType = MetricType.COSINE, params = {"M=16", "efConstruction=200"}),
        @VectorIndex(field = "tenantId", name = "idx_tenant_id", type = IndexType.INVERTED)
})
public class KnowledgeChunk {

    /** 业务主键，对应 Milvus 字段 chunk_id。 */
    @VectorId(name = "chunk_id", type = VectorDataType.VARCHAR, maxLength = 128)
    private String chunkId;

    /** 租户 ID，可以用于搜索和删除时过滤。 */
    @VectorField(name = "tenant_id", filterable = true)
    private Long tenantId;

    /** 标题，默认会在搜索结果中返回。 */
    @VectorField(maxLength = 512)
    private String title;

    /** 需要生成向量的正文内容。 */
    @VectorEmbeddingText(vectorField = "embedding")
    @VectorField(maxLength = 8192)
    private String content;

    /** 向量字段，通常不需要返回给前端，所以 output=false。 */
    @VectorField(type = VectorDataType.FLOAT_VECTOR, dimension = 1024,
            metricType = MetricType.COSINE, output = false)
    private List<Float> embedding;
}
```

### 4. 注解说明

| 注解 | 放在哪里 | 作用 | 常用参数 |
| --- | --- | --- | --- |
| `@VectorCollection` | 类 | 声明一个 domain 对应一个 Milvus Collection | `name`、`database`、`description`、`autoCreate`、`autoCreateIndex` |
| `@VectorId` | 字段 | 声明主键字段 | `name`、`type`、`maxLength`、`autoId` |
| `@VectorField` | 字段 | 声明普通字段或向量字段 | `name`、`type`、`maxLength`、`dimension`、`filterable`、`output` |
| `@VectorEmbeddingText` | 文本字段 | 声明这个字段需要生成 embedding | `vectorField`、`provider`、`model` |
| `@VectorIndex` | 类或字段 | 声明索引 | `field`、`name`、`type`、`metricType`、`params` |
| `@VectorIndexes` | 类 | 声明多个索引 | 多个 `@VectorIndex` |
| `@VectorIgnore` | 字段 | 忽略字段，不写入 Milvus | 无 |
| `@VectorPartitionKey` | 字段 | 声明 Milvus 分区键 | 无 |

| 枚举 | 可选值 |
| --- | --- |
| `VectorDataType` | `AUTO`、`BOOL`、`INT8`、`INT16`、`INT32`、`INT64`、`FLOAT`、`DOUBLE`、`VARCHAR`、`JSON`、`ARRAY`、`FLOAT_VECTOR`、`BINARY_VECTOR`、`SPARSE_FLOAT_VECTOR` |
| `IndexType` | `HNSW`、`IVF_FLAT`、`IVF_SQ8`、`AUTOINDEX`、`INVERTED` |
| `MetricType` | `COSINE`、`L2`、`IP` |

### 5. 创建 Collection

```java
import com.rongqi.vector.core.VectorTemplate;
import org.springframework.stereotype.Service;

/**
 * Collection 初始化示例。
 */
@Service
public class VectorInitService {

    private final VectorTemplate vectorTemplate;

    public VectorInitService(VectorTemplate vectorTemplate) {
        this.vectorTemplate = vectorTemplate;
    }

    public void init() {
        // 根据 KnowledgeChunk 上的注解创建或校验 Milvus Collection。
        vectorTemplate.ensureCollection(KnowledgeChunk.class);
    }
}
```

### 6. 写入或更新数据

```java
KnowledgeChunk chunk = new KnowledgeChunk();
chunk.setChunkId("doc_001_chunk_001");
chunk.setTenantId(1001L);
chunk.setTitle("发票申请说明");
chunk.setContent("登录系统后进入订单页面，选择需要开票的订单并提交发票信息。");

// 如果 embedding 字段为空，框架会读取 content 并调用 EmbeddingProvider 自动生成向量。
vectorTemplate.upsert(chunk);
```

### 7. 向量搜索

```java
KnowledgeChunk filter = new KnowledgeChunk();
filter.setTenantId(1001L);

SearchResult<KnowledgeChunk> result = vectorTemplate.search(
        KnowledgeChunk.class,
        "如何申请发票",
        filter,
        SearchOptions.builder()
                .topK(5)
                .minScore(0.3)
                .outputFields("chunk_id", "tenant_id", "title", "content")
                .build()
);
```

如果希望在向量相似度之外叠加业务排序，可以使用 `candidateTopK` 和 `RankOptions`。例如先从 Milvus 召回 50 条候选，再根据 `priority` 字段加权排序，最终返回 10 条：

```java
import com.rongqi.vector.core.rank.RankOptions;

SearchResult<KnowledgeChunk> result = vectorTemplate.search(
        KnowledgeChunk.class,
        "如何申请发票",
        filter,
        SearchOptions.builder()
                .topK(10)
                .candidateTopK(50)
                .rank(RankOptions.builder()
                        .fieldBoost("priority", 0.2)
                        .build())
                .outputFields("chunk_id", "tenant_id", "title", "content", "priority")
                .build()
);
```

排序公式为 `rankScore = score + 字段值 * 权重`。`score` 仍然表示 Milvus 原始相似度分数，`rankScore` 表示二次排序后的分数。字段加权目前只处理数字字段；字段缺失或不是数字时会自动忽略。参与排序的字段需要在结果实体中可读取，HTTP collection 模式建议把该字段加入 `outputFields`。

复杂过滤条件建议写在 `SearchOptions` 中。`filter` 对象仍然兼容旧用法，只适合表达“字段等于某个值”；`SearchOptions` 支持大于、小于、列表包含、列表排除和模糊匹配。

```java
SearchResult<KnowledgeChunk> result = vectorTemplate.search(
        KnowledgeChunk.class,
        "如何申请发票",
        null,
        SearchOptions.builder()
                .topK(5)
                .gte("tenantId", 1000L)
                .lt("tenantId", 2000L)
                .in("tenantId", List.of(1001L, 1002L))
                .notIn("tenantId", List.of(9999L))
                .like("title", "发票")
                .outputFields("chunk_id", "tenant_id", "title", "content")
                .build()
);
```

可用的复杂过滤方法如下：

| 方法 | 含义 | 示例 |
| --- | --- | --- |
| `eq(field, value)` | 等于 | `.eq("tenantId", 1001L)` |
| `ne(field, value)` | 不等于 | `.ne("tenantId", 1002L)` |
| `gt(field, value)` | 大于 | `.gt("tenantId", 1000L)` |
| `gte(field, value)` | 大于等于 | `.gte("tenantId", 1000L)` |
| `lt(field, value)` | 小于 | `.lt("tenantId", 2000L)` |
| `lte(field, value)` | 小于等于 | `.lte("tenantId", 2000L)` |
| `in(field, values)` | 在列表中 | `.in("tenantId", List.of(1001L, 1002L))` |
| `notIn(field, values)` | 不在列表中 | `.notIn("tenantId", List.of(9999L))` |
| `like(field, pattern)` | 模糊匹配，未包含 `%` 时会自动按包含匹配处理 | `.like("title", "发票")` |

注意：过滤字段必须是主键字段，或者在注解中配置了 `filterable = true`。如果要对 `title` 做模糊匹配，需要把字段定义为 `@VectorField(maxLength = 512, filterable = true)`。

### 8. 删除数据

```java
// 按主键删除。
vectorTemplate.deleteById(KnowledgeChunk.class, "doc_001_chunk_001");

// 按条件删除，例如删除某个租户的数据。
KnowledgeChunk filter = new KnowledgeChunk();
filter.setTenantId(1001L);
vectorTemplate.delete(KnowledgeChunk.class, filter);
```

### 9. 自定义 EmbeddingProvider

如果内置 HTTP Provider 不满足需求，可以自己实现 `EmbeddingProvider`，例如接入 Ollama、本地模型、公司内部模型服务。

```java
import com.rongqi.vector.embedding.EmbeddingOptions;
import com.rongqi.vector.embedding.EmbeddingProvider;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 自定义 Embedding 模型示例。
 * 只要注册成 Spring Bean，RongQi Vector 会自动加入 Provider 注册表。
 */
@Component
public class LocalBgeEmbeddingProvider implements EmbeddingProvider {

    @Override
    public String name() {
        return "local-bge";
    }

    @Override
    public List<List<Float>> embed(List<String> texts, EmbeddingOptions options) {
        // 这里调用你自己的模型服务，然后返回和 texts 数量一致的向量列表。
        throw new UnsupportedOperationException("请接入自己的 Embedding 服务");
    }

    @Override
    public int dimension(EmbeddingOptions options) {
        // 这里返回模型实际输出维度，必须和 @VectorField(dimension=...) 一致。
        return 1024;
    }
}
```

使用自定义 Provider：

```yaml
rongqi:
  vector:
    embedding:
      default-provider: local-bge
```

也可以在字段上单独指定：

```java
@VectorEmbeddingText(vectorField = "embedding", provider = "local-bge", model = "bge-large-zh")
private String content;
```

### 7. 文本切块工具

长文档写入向量库前，建议先切成较小的文本片段，再分别生成 embedding。`vector-core` 提供了不依赖 Milvus 的通用切块工具：

```java
import com.rongqi.vector.core.text.TextChunk;
import com.rongqi.vector.core.text.TextChunkOptions;
import com.rongqi.vector.core.text.TextChunker;
import java.util.List;

TextChunkOptions options = TextChunkOptions.builder()
        .maxChars(800)
        .overlapChars(100)
        .build();

List<TextChunk> chunks = TextChunker.split(documentText, options);
for (TextChunk chunk : chunks) {
    String chunkId = "doc_001_chunk_" + chunk.getIndex();
    String content = chunk.getContent();
    int start = chunk.getStart();
    int end = chunk.getEnd();
    // 业务方可以把 chunkId、content、start、end 一起写入自己的 domain 或 HTTP items。
}
```

默认参数是 `maxChars=800`、`overlapChars=100`。切块时会优先在换行处切分，其次在空白字符处切分；找不到合适边界时才按最大长度硬切。重叠窗口可以减少片段边界处语义丢失。

## 方式二：独立 HTTP 服务模式

这种方式适合不想引入 Java 依赖、不会 Java 开发、或者只想通过 Apipost/Postman/curl 调用的用户。

### 1. 打包并启动服务

```powershell
mvn -DskipTests package
java -jar vector-server\target\vector-server-0.1.0-SNAPSHOT.jar
```

如果本机 Maven 默认使用 JDK 8，需要先切换到 JDK 17：

```powershell
$env:JAVA_HOME='D:\jdk\jdk17'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn -DskipTests package
java -jar vector-server\target\vector-server-0.1.0-SNAPSHOT.jar
```

默认服务地址：

```text
http://127.0.0.1:18080
```

### 2. HTTP 服务环境变量

生产环境不要把 Milvus token 或模型 key 写死在 `application.yml`，建议全部通过环境变量传入。

| 环境变量 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `SERVER_PORT` | 否 | `18080` | HTTP 服务端口 |
| `RONGQI_VECTOR_MILVUS_URI` | 是 | `http://127.0.0.1:19530` | Milvus 地址 |
| `RONGQI_VECTOR_MILVUS_TOKEN` | 视 Milvus 而定 | 空 | Milvus token |
| `RONGQI_VECTOR_MILVUS_DATABASE` | 否 | `default` | 默认 database |
| `RONGQI_VECTOR_EMBEDDING_DEFAULT_PROVIDER` | 是 | `noop` | 默认 EmbeddingProvider 名称，真实搜索建议设置为 `http` 或自定义名称 |
| `RONGQI_VECTOR_EMBEDDING_BATCH_SIZE` | 否 | `32` | 批量写入时每次调用 EmbeddingProvider 的文本数量 |
| `RONGQI_VECTOR_HTTP_EMBEDDING_ENABLED` | 使用 HTTP Provider 时必填 | `false` | 是否启用内置 HTTP EmbeddingProvider |
| `RONGQI_VECTOR_HTTP_EMBEDDING_NAME` | 否 | `http` | HTTP Provider 名称 |
| `RONGQI_VECTOR_HTTP_EMBEDDING_URL` | 使用 HTTP Provider 时必填 | 空 | Embedding 服务地址 |
| `RONGQI_VECTOR_HTTP_EMBEDDING_API_KEY` | 视模型而定 | 空 | Embedding 服务密钥 |
| `RONGQI_VECTOR_HTTP_EMBEDDING_MODEL` | 使用 HTTP Provider 时必填 | 空 | Embedding 模型名称 |
| `RONGQI_VECTOR_HTTP_EMBEDDING_DIMENSION` | 是 | `1024` | 模型输出维度 |
| `RONGQI_VECTOR_HTTP_EMBEDDING_TIMEOUT_MILLIS` | 否 | `30000` | Embedding 请求超时时间 |
| `RONGQI_VECTOR_HTTP_EMBEDDING_MAX_RETRIES` | 否 | `2` | Embedding 请求遇到 429、5xx 或网络异常时的最大重试次数 |
| `RONGQI_VECTOR_HTTP_EMBEDDING_RETRY_INTERVAL_MILLIS` | 否 | `500` | Embedding 每次重试前等待的毫秒数 |
| `RONGQI_VECTOR_SCHEMA_TYPE` | 否 | `file` | HTTP Collection schema 存储方式，支持 `file`、`jdbc` |
| `RONGQI_VECTOR_SCHEMA_STORAGE_DIR` | 否 | `data/rongqi-vector/collections` | HTTP Collection schema 保存目录 |
| `RONGQI_VECTOR_SCHEMA_JDBC_TABLE_NAME` | 否 | `vector_collection_schema` | JDBC schema 表名 |
| `RONGQI_VECTOR_SCHEMA_JDBC_INITIALIZE_SCHEMA` | 否 | `true` | JDBC 模式是否自动建表 |

DashScope OpenAI 兼容模式示例：

```powershell
$env:RONGQI_VECTOR_EMBEDDING_DEFAULT_PROVIDER='http'
$env:RONGQI_VECTOR_HTTP_EMBEDDING_ENABLED='true'
$env:RONGQI_VECTOR_HTTP_EMBEDDING_NAME='http'
$env:RONGQI_VECTOR_HTTP_EMBEDDING_URL='https://dashscope.aliyuncs.com/compatible-mode/v1'
$env:RONGQI_VECTOR_HTTP_EMBEDDING_API_KEY='你的真实key'
$env:RONGQI_VECTOR_HTTP_EMBEDDING_MODEL='text-embedding-v4'
$env:RONGQI_VECTOR_HTTP_EMBEDDING_DIMENSION='1024'
```

### 3. HTTP 用户标准流程

| 步骤 | 接口 | 作用 |
| --- | --- | --- |
| 1 | `POST /api/vector/collections/ensure` | 创建或注册 Collection，定义字段、索引、Embedding 映射 |
| 2 | `POST /api/vector/documents/upsert` | 写入或更新数据 |
| 3 | `POST /api/vector/search` | 通过文本或向量检索数据 |
| 4 | `POST /api/vector/documents/delete` | 通过主键或条件删除数据 |

`docs/rongqi-vector-apipost-openapi.json` 可以导入 Apipost。  
`docs/http-request-examples.md` 里也保留了单独的 HTTP 请求示例。

## HTTP 接口统一响应

所有 HTTP 接口都返回统一结构：

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "success",
  "data": {}
}
```

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| `success` | boolean | 是否成功 |
| `code` | string | 结果编码，成功固定为 `SUCCESS`，失败为具体错误码 |
| `message` | string | 结果说明或错误原因 |
| `data` | object | 业务数据，失败时一般为 `null` |

## HTTP 接口 1：健康检查

```http
GET /api/vector/health
```

用于确认服务是否启动。

### 响应示例

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "success",
  "data": {
    "status": "UP"
  }
}
```

## HTTP 接口 2：诊断 domain

```http
GET /api/vector/diagnose?domain=com.example.demo.domain.KnowledgeChunk
```

这个接口主要给 Maven 注解模式使用，用来检查 domain 注解、Collection、索引、EmbeddingProvider 是否配置正确。

诊断结果会包含 Collection 名称、字段数量、索引数量、可过滤字段、默认输出字段、默认向量字段、EmbeddingProvider 是否已注册，以及 Milvus 中 Collection 是否存在、是否已加载。新项目接入失败时，建议先调用该接口确认配置和 Milvus 状态。

| 参数 | 位置 | 类型 | 必填 | 说明 | 示例 |
| --- | --- | --- | --- | --- | --- |
| `domain` | Query | string | 是 | Java domain 完整类名 | `com.example.demo.domain.KnowledgeChunk` |

## HTTP 接口 3：创建或注册 Collection

```http
POST /api/vector/collections/ensure
Content-Type: application/json
```

HTTP 模式必须先调用这个接口。Milvus 中已经有 Collection 不代表 RongQi Vector 知道字段、索引和 Embedding 映射，所以需要通过这个接口注册 schema。

### 请求示例

```json
{
  "database": "knowledge_base",
  "collection": "knowledge_chunk_v1",
  "description": "知识片段向量集合",
  "autoCreate": true,
  "autoCreateIndex": true,
  "validateSchema": true,
  "dynamicFieldEnabled": false,
  "fields": [
    {
      "name": "chunk_id",
      "type": "VARCHAR",
      "primaryKey": true,
      "maxLength": 128,
      "filterable": true
    },
    {
      "name": "tenant_id",
      "type": "INT64",
      "filterable": true
    },
    {
      "name": "content",
      "type": "VARCHAR",
      "maxLength": 8192
    },
    {
      "name": "embedding",
      "type": "FLOAT_VECTOR",
      "dimension": 1024,
      "metricType": "COSINE",
      "output": false
    }
  ],
  "indexes": [
    {
      "field": "embedding",
      "name": "idx_embedding",
      "type": "HNSW",
      "metricType": "COSINE",
      "params": {
        "M": 16,
        "efConstruction": 200
      }
    },
    {
      "field": "tenant_id",
      "name": "idx_tenant_id",
      "type": "INVERTED"
    }
  ],
  "embeddings": [
    {
      "textField": "content",
      "vectorField": "embedding",
      "provider": "http",
      "model": "text-embedding-v4"
    }
  ]
}
```

### 顶层参数

| 参数 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `database` | string | 否 | 全局默认 database | Milvus database 名称 |
| `collection` | string | 是 | 无 | Milvus Collection 名称 |
| `description` | string | 否 | 空 | Collection 描述 |
| `autoCreate` | boolean | 否 | `true` | Collection 不存在时是否自动创建 |
| `autoCreateIndex` | boolean | 否 | `true` | 索引不存在时是否自动创建 |
| `validateSchema` | boolean | 否 | `true` | Collection 已存在时是否校验 schema |
| `dynamicFieldEnabled` | boolean | 否 | `false` | 是否启用 Milvus 动态字段 |
| `fields` | array | 是 | 空数组 | 字段列表，至少包含一个主键字段和一个向量字段 |
| `indexes` | array | 否 | 空数组 | 索引列表，建议向量字段必须建索引 |
| `embeddings` | array | 使用文本自动向量化时必填 | 空数组 | 文本字段到向量字段的映射 |

`validateSchema=true` 时，如果 Milvus 中已存在同名 Collection，框架会校验动态字段开关、字段是否缺失、字段类型、主键、autoId、`VARCHAR.maxLength` 和向量维度。校验失败会直接报错，避免代码 schema 和 Milvus 真实 schema 不一致时继续写入错误数据。

### fields 参数

| 参数 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `name` | string | 是 | 无 | Milvus 字段名 |
| `type` | string | 是 | `AUTO` | 字段类型，可选值见 `VectorDataType` |
| `primaryKey` | boolean | 否 | `false` | 是否主键 |
| `autoId` | boolean | 否 | `false` | 主键是否由 Milvus 自动生成 |
| `maxLength` | number | `VARCHAR` 必填 | `-1` | 字符串最大长度 |
| `dimension` | number | 向量字段必填 | `-1` | 向量维度，必须和 Embedding 模型输出维度一致 |
| `metricType` | string | 向量字段建议填写 | `COSINE` | 相似度算法，可选 `COSINE`、`L2`、`IP` |
| `nullable` | boolean | 否 | `true` | 是否允许为空 |
| `filterable` | boolean | 否 | `false` | 是否允许作为查询或删除条件 |
| `output` | boolean | 否 | `true` | 搜索结果是否默认返回该字段，向量字段建议为 `false` |

### indexes 参数

| 参数 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `field` | string | 是 | 无 | 要创建索引的字段名 |
| `name` | string | 否 | 自动生成 | 索引名称 |
| `type` | string | 是 | `INVERTED` | 索引类型，可选 `HNSW`、`IVF_FLAT`、`IVF_SQ8`、`AUTOINDEX`、`INVERTED` |
| `metricType` | string | 向量索引建议填写 | `COSINE` | 相似度算法 |
| `params` | object | 否 | 空对象 | 索引参数，例如 HNSW 的 `M`、`efConstruction` |

### embeddings 参数

| 参数 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `textField` | string | 是 | 无 | 需要生成 embedding 的文本字段 |
| `vectorField` | string | 是 | 无 | embedding 生成后写入的向量字段 |
| `provider` | string | 否 | 默认 Provider | EmbeddingProvider 名称，例如 `http`、`local-bge` |
| `model` | string | 否 | Provider 默认模型 | 指定 Embedding 模型名称 |

### 响应示例

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "success",
  "data": {
    "count": 1
  }
}
```

## HTTP 接口 4：写入或更新数据

```http
POST /api/vector/documents/upsert
Content-Type: application/json
```

写入数据时，如果请求数据里没有传向量字段，但 Collection 已配置 `embeddings`，框架会读取文本字段并自动调用 EmbeddingProvider 生成向量。

### collection 模式请求示例

```json
{
  "collection": "knowledge_chunk_v1",
  "items": [
    {
      "chunk_id": "doc_001_chunk_001",
      "tenant_id": 1001,
      "content": "登录系统后进入订单页面，选择需要开票的订单并提交发票信息。"
    }
  ]
}
```

### domain 模式请求示例

```json
{
  "domain": "com.example.demo.domain.KnowledgeChunk",
  "items": [
    {
      "chunkId": "doc_001_chunk_001",
      "tenantId": 1001,
      "content": "登录系统后进入订单页面，选择需要开票的订单并提交发票信息。"
    }
  ]
}
```

### 参数说明

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `domain` | string | domain 模式必填 | Java domain 完整类名，和 `collection` 二选一 |
| `collection` | string | collection 模式必填 | HTTP 注册过的 Collection 名称，和 `domain` 二选一 |
| `items` | array | 是 | 要写入的数据列表 |
| `items[*]` | object | 是 | 单条业务数据 |

| 模式 | items 字段名怎么写 | 示例 |
| --- | --- | --- |
| domain 模式 | 使用 Java 字段名 | `chunkId`、`tenantId`、`content` |
| collection 模式 | 使用 `/collections/ensure` 中定义的字段名 | `chunk_id`、`tenant_id`、`content` |

### 响应示例

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "success",
  "data": {
    "count": 1
  }
}
```

## HTTP 接口 5：向量搜索

```http
POST /api/vector/search
Content-Type: application/json
```

`query` 和 `vector` 二选一：

- 传 `query`：框架会先调用 EmbeddingProvider 把文本转成向量，再搜索。
- 传 `vector`：调用方已经自己生成好向量，框架直接搜索。

### 文本搜索示例

```json
{
  "collection": "knowledge_chunk_v1",
  "query": "如何申请发票",
  "topK": 5,
  "minScore": 0.3,
  "filterObject": {
    "tenant_id": 1001
  },
  "outputFields": ["chunk_id", "tenant_id", "content"]
}
```

### 带业务排序搜索示例

下面示例表示先召回 50 条候选，再按 `score + priority * 0.2` 重新排序，最终返回前 10 条：

```json
{
  "collection": "knowledge_chunk_v1",
  "query": "如何申请发票",
  "topK": 10,
  "candidateTopK": 50,
  "rank": {
    "fieldBoosts": [
      { "field": "priority", "weight": 0.2 }
    ]
  },
  "outputFields": ["chunk_id", "tenant_id", "title", "content", "priority"]
}
```

### 复杂过滤搜索示例

`filterObject` 适合简单等值过滤；需要大于、小于、列表包含、列表排除或模糊匹配时，使用 `filters`。

```json
{
  "collection": "knowledge_chunk_v1",
  "query": "如何申请发票",
  "topK": 5,
  "filters": [
    { "field": "tenant_id", "operator": "GTE", "value": 1000 },
    { "field": "tenant_id", "operator": "LT", "value": 2000 },
    { "field": "tenant_id", "operator": "IN", "value": [1001, 1002] },
    { "field": "tenant_id", "operator": "NOT_IN", "value": [9999] },
    { "field": "title", "operator": "LIKE", "value": "发票" }
  ],
  "outputFields": ["chunk_id", "tenant_id", "title", "content"]
}
```

### 直接传向量搜索示例

```json
{
  "collection": "knowledge_chunk_v1",
  "vector": [0.012, 0.034, 0.056],
  "topK": 5,
  "filterObject": {
    "tenant_id": 1001
  }
}
```

真实请求里 `vector` 的长度必须等于 Collection 向量字段维度，例如 1024。

### 参数说明

| 参数 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `domain` | string | domain 模式必填 | 无 | Java domain 完整类名，和 `collection` 二选一 |
| `collection` | string | collection 模式必填 | 无 | HTTP 注册过的 Collection 名称，和 `domain` 二选一 |
| `query` | string | 和 `vector` 二选一 | 无 | 搜索文本，会自动生成 embedding |
| `vector` | array | 和 `query` 二选一 | 无 | 已生成好的向量 |
| `filterObject` | object | 否 | 空对象 | 过滤条件，只会使用非空字段，例如只搜索 `tenant_id=1001` 的数据 |
| `filters` | array | 否 | 空数组 | 复杂过滤条件，支持 `EQ`、`NE`、`GT`、`GTE`、`LT`、`LTE`、`IN`、`NOT_IN`、`LIKE` |
| `topK` | number | 否 | `10` | 返回最相似的前 N 条 |
| `candidateTopK` | number | 否 | 等于 `topK` | 第一阶段向量召回候选数量，启用二次排序时建议大于 `topK` |
| `minScore` | number | 否 | 无 | 最低分数，小于该分数的结果会被过滤 |
| `outputFields` | array | 否 | schema 默认输出字段 | 指定返回字段；为空时返回 `output=true` 的字段 |
| `rank` | object | 否 | 无 | 二次排序配置，目前支持字段加权 |
| `rank.fieldBoosts` | array | 否 | 空数组 | 字段加权列表，计算公式为 `rankScore = score + 字段值 * weight` |
| `rank.fieldBoosts[].field` | string | 是 | 无 | 参与排序的字段名，HTTP collection 模式使用 schema 字段名，domain 模式使用 Java 字段名 |
| `rank.fieldBoosts[].weight` | number | 是 | 无 | 字段权重，正数提高排名，负数降低排名 |

| 模式 | `filterObject` / `filters.field` 字段名怎么写 | 示例 |
| --- | --- | --- |
| domain 模式 | 使用 Java 字段名 | `{ "tenantId": 1001 }` |
| collection 模式 | 使用 `/collections/ensure` 中定义的字段名 | `{ "tenant_id": 1001 }` |

`filters` 中每个对象包含三个字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `field` | string | 过滤字段名 |
| `operator` | string | 过滤操作符：`EQ`、`NE`、`GT`、`GTE`、`LT`、`LTE`、`IN`、`NOT_IN`、`LIKE` |
| `value` | any | 过滤值；`IN` 和 `NOT_IN` 必须传数组，`LIKE` 必须传字符串。`LIKE` 值不包含 `%` 时会自动按包含匹配处理 |

注意：过滤字段必须是主键字段，或者在 schema 中配置了 `filterable=true`。模糊匹配由 Milvus filter 表达式执行，通常用于标量字符串字段；大段正文建议优先通过向量召回，不建议直接做大范围模糊过滤。

### 响应示例

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "success",
  "data": {
    "hits": [
      {
        "score": 0.82,
        "entity": {
          "chunk_id": "doc_001_chunk_001",
          "tenant_id": 1001,
          "content": "登录系统后进入订单页面，选择需要开票的订单并提交发票信息。"
        }
      }
    ]
  }
}
```

| 响应参数 | 类型 | 说明 |
| --- | --- | --- |
| `data.hits` | array | 搜索命中列表 |
| `data.hits[*].score` | number | 相似度分数 |
| `data.hits[*].entity` | object | 命中的业务数据 |

## HTTP 接口 6：删除数据

```http
POST /api/vector/documents/delete
Content-Type: application/json
```

删除支持两种方式：

- 按主键删除：传 `ids`。
- 按条件删除：传 `filterObject` 或 `filters`。

如果同时传 `ids` 和条件，接口会优先按 `ids` 删除。

### 按主键删除示例

```json
{
  "collection": "knowledge_chunk_v1",
  "ids": ["doc_001_chunk_001"]
}
```

### 按条件删除示例

```json
{
  "collection": "knowledge_chunk_v1",
  "filterObject": {
    "tenant_id": 1001
  }
}
```

### 按复杂条件删除示例

```json
{
  "collection": "knowledge_chunk_v1",
  "filters": [
    { "field": "tenant_id", "operator": "GTE", "value": 1000 },
    { "field": "tenant_id", "operator": "LT", "value": 2000 },
    { "field": "business_code", "operator": "NOT_IN", "value": ["draft"] }
  ]
}
```

### 参数说明

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `domain` | string | domain 模式必填 | Java domain 完整类名，和 `collection` 二选一 |
| `collection` | string | collection 模式必填 | HTTP 注册过的 Collection 名称，和 `domain` 二选一 |
| `ids` | array | 和 `filterObject`、`filters` 至少传一个 | 主键列表 |
| `filterObject` | object | 和 `ids`、`filters` 至少传一个 | 简单等值删除条件，只会使用非空字段 |
| `filters` | array | 和 `ids`、`filterObject` 至少传一个 | 复杂删除条件，支持 `EQ`、`NE`、`GT`、`GTE`、`LT`、`LTE`、`IN`、`NOT_IN`、`LIKE` |

| 模式 | filterObject 字段名怎么写 | 示例 |
| --- | --- | --- |
| domain 模式 | 使用 Java 字段名 | `{ "tenantId": 1001 }` |
| collection 模式 | 使用 `/collections/ensure` 中定义的字段名 | `{ "tenant_id": 1001 }` |

### 响应示例

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "success",
  "data": {
    "count": 1
  }
}
```

## Embedding HTTP 服务格式

内置 HTTP EmbeddingProvider 使用 OpenAI 兼容格式。

### 请求格式

```json
{
  "model": "text-embedding-v4",
  "input": [
    "登录系统后进入订单页面，选择需要开票的订单并提交发票信息。"
  ]
}
```

### 响应格式

```json
{
  "data": [
    {
      "embedding": [0.012, 0.034, 0.056]
    }
  ]
}
```

如果配置的是 base url，例如：

```text
https://dashscope.aliyuncs.com/compatible-mode/v1
```

框架会自动请求：

```text
https://dashscope.aliyuncs.com/compatible-mode/v1/embeddings
```

## 常见问题

### 1. `VECTOR_EMBEDDING_PROVIDER_NOT_FOUND: http`

原因：请求里使用了 `provider: "http"`，但服务没有启用 HTTP EmbeddingProvider。

处理方式：

```powershell
$env:RONGQI_VECTOR_EMBEDDING_DEFAULT_PROVIDER='http'
$env:RONGQI_VECTOR_HTTP_EMBEDDING_ENABLED='true'
$env:RONGQI_VECTOR_HTTP_EMBEDDING_NAME='http'
$env:RONGQI_VECTOR_HTTP_EMBEDDING_URL='你的Embedding服务地址'
$env:RONGQI_VECTOR_HTTP_EMBEDDING_MODEL='你的模型名称'
```

然后重启服务。

### 2. `collection 未注册，请先调用 /api/vector/collections/ensure`

原因：Milvus 中有 Collection，但 RongQi Vector 服务没有加载到 schema。

处理方式：

- 先调用 `/api/vector/collections/ensure`。
- 如果使用 `file` 模式，检查 `RONGQI_VECTOR_SCHEMA_STORAGE_DIR` 目录下是否存在对应 schema 文件，并确认服务重启后能读取该目录。
- 如果使用 `jdbc` 模式，检查 `RONGQI_VECTOR_SCHEMA_TYPE=jdbc`、`spring.datasource.*`、`RONGQI_VECTOR_SCHEMA_JDBC_TABLE_NAME` 是否配置正确，并确认表中存在对应 collection 的 schema 记录。

### 3. 向量维度不一致

原因：Embedding 模型输出维度和字段定义里的 `dimension` 不一致。

处理方式：

- `text-embedding-v4` 如果使用 1024 维，字段 `dimension` 就写 `1024`。
- 更换模型后，要同步修改 Collection 字段维度。
- 已经创建过的 Milvus Collection 不能随意改向量维度，通常需要新建 Collection。

### 4. 搜索没有结果

排查顺序：

| 排查项 | 说明 |
| --- | --- |
| Collection 是否已创建 | 先调用 `/api/vector/collections/ensure` |
| 数据是否写入成功 | 查看 `/api/vector/documents/upsert` 的 `data.count` |
| EmbeddingProvider 是否正常 | 检查模型 key、url、model、dimension |
| 过滤条件是否太严格 | 暂时去掉 `filterObject` 再搜索 |
| `minScore` 是否太高 | 暂时去掉 `minScore` 再搜索 |

### 5. 生产环境可以直接用 `noop` Provider 吗

不建议。`noop` 只适合本地调试或占位。真实向量搜索应该配置 `http` Provider 或自定义 Provider。

## 构建与测试

项目要求 JDK 17+。

```powershell
mvn test
```

如果本机 Maven 默认使用 JDK 8：

```powershell
$env:JAVA_HOME='D:\jdk\jdk17'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn test
```

打包：

```powershell
mvn -DskipTests package
```

启动 HTTP 服务：

```powershell
java -jar vector-server\target\vector-server-0.1.0-SNAPSHOT.jar
```

## 开发规范



当前项目约定：

- 代码注释使用中文，方便新同事和初学者理解。
- DTO、domain 推荐使用 Lombok 生成 getter/setter，减少样板代码。
- 不在配置文件里写死真实 token、api key。
- 不在 Spring Boot 配置里硬编码业务 Collection；Maven 模式使用注解，HTTP 模式使用接口注册。
- 新增功能优先补充示例和接口文档。
- 修改 HTTP 行为时同步更新 `docs/rongqi-vector-apipost-openapi.json` 和本 README。
