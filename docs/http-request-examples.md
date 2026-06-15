# RongQi Vector HTTP 请求示例

以下示例可以直接复制到 Apipost 中测试。

## 1. 纯 HTTP Collection 模式

适合不会 Java 或不想引入 Maven SDK 的用户。使用顺序：

1. 创建 Collection。
2. 写入或更新数据。
3. 向量搜索。
4. 删除数据。

重要说明：

- Milvus 中存在 `knowledge_chunk_v1` 不代表 RongQi Vector 服务已经注册了它的字段、索引和 embedding 映射。
- 第一次使用或服务重启后，建议先调用 `POST /api/vector/collections/ensure`。
- 服务会把 schema 持久化到配置的 schema store 中，默认是 `file` 模式，目录为 `data/rongqi-vector/collections`，重启后会自动加载。
- `file` 模式适合开发和单机测试；多实例生产环境建议配置 `rongqi.vector.schema.type=jdbc`，把 schema 存储到数据库。

### 1.1 创建 Collection

```http
POST http://127.0.0.1:18080/api/vector/collections/ensure
Content-Type: application/json
```

```json
{
  "database": "knowledge_base",
  "collection": "knowledge_chunk_v1",
  "description": "知识片段向量集合",
  "autoCreate": true,
  "autoCreateIndex": true,
  "validateSchema": true,
  "fields": [
    {
      "name": "chunk_id",
      "type": "VARCHAR",
      "primaryKey": true,
      "autoId": false,
      "maxLength": 128,
      "filterable": true,
      "output": true
    },
    {
      "name": "tenant_id",
      "type": "INT64",
      "filterable": true,
      "output": true
    },
    {
      "name": "biz_id",
      "type": "VARCHAR",
      "maxLength": 128,
      "filterable": true,
      "output": true
    },
    {
      "name": "business_code",
      "type": "VARCHAR",
      "maxLength": 64,
      "filterable": true,
      "output": true
    },
    {
      "name": "title",
      "type": "VARCHAR",
      "maxLength": 512,
      "output": true
    },
    {
      "name": "content",
      "type": "VARCHAR",
      "maxLength": 8192,
      "output": true
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
    },
    {
      "field": "business_code",
      "name": "idx_business_code",
      "type": "INVERTED"
    }
  ],
  "embeddings": [
    {
      "textField": "content",
      "vectorField": "embedding",
      "provider": "http",
      "model": "bge-large-zh"
    }
  ]
}
```

### 1.2 写入或更新数据

如果配置了 `embeddings`，并且请求中没有传 `embedding` 字段，服务会自动读取 `content` 生成向量。

```http
POST http://127.0.0.1:18080/api/vector/documents/upsert
Content-Type: application/json
```

```json
{
  "collection": "knowledge_chunk_v1",
  "items": [
    {
      "chunk_id": "doc_001_chunk_001",
      "tenant_id": 1001,
      "biz_id": "doc-001",
      "business_code": "faq",
      "title": "发票申请说明",
      "content": "登录系统后进入订单页面，选择需要开票的订单并提交发票信息。"
    },
    {
      "chunk_id": "doc_002_chunk_001",
      "tenant_id": 1001,
      "biz_id": "doc-002",
      "business_code": "faq",
      "title": "密码重置说明",
      "content": "用户可以在登录页面点击忘记密码，通过手机号验证码完成密码重置。"
    }
  ]
}
```

如果调用方已经生成好了向量，也可以直接传 `embedding`，服务不会再调用 embedding provider：

```json
{
  "collection": "knowledge_chunk_v1",
  "items": [
    {
      "chunk_id": "doc_003_chunk_001",
      "tenant_id": 1001,
      "biz_id": "doc-003",
      "business_code": "manual",
      "title": "人工客服说明",
      "content": "用户可以在工作日联系人工客服处理复杂问题。",
      "embedding": [0.01, 0.02, 0.03, 0.04]
    }
  ]
}
```

注意：上面 `embedding` 只是示例，真实向量长度必须等于 Collection 定义的 `dimension`，例如 1024。

### 1.3 向量搜索

按文本搜索：

```http
POST http://127.0.0.1:18080/api/vector/search
Content-Type: application/json
```

```json
{
  "collection": "knowledge_chunk_v1",
  "query": "如何申请发票",
  "topK": 5,
  "minScore": 0.2,
  "outputFields": ["chunk_id", "tenant_id", "biz_id", "business_code", "title", "content"],
  "filterObject": {
    "tenant_id": 1001,
    "business_code": "faq"
  }
}
```

复杂过滤搜索：

```json
{
  "collection": "knowledge_chunk_v1",
  "query": "如何申请发票",
  "topK": 5,
  "outputFields": ["chunk_id", "tenant_id", "title", "content"],
  "filters": [
    { "field": "tenant_id", "operator": "GTE", "value": 1000 },
    { "field": "tenant_id", "operator": "LT", "value": 2000 },
    { "field": "business_code", "operator": "IN", "value": ["faq", "invoice"] },
    { "field": "business_code", "operator": "NOT_IN", "value": ["draft"] },
    { "field": "title", "operator": "LIKE", "value": "%发票%" }
  ]
}
```

`filters.operator` 支持 `EQ`、`NE`、`GT`、`GTE`、`LT`、`LTE`、`IN`、`NOT_IN`、`LIKE`。过滤字段必须是主键字段，或者在 collection schema 中配置 `filterable=true`。

按已有向量搜索：

```json
{
  "collection": "knowledge_chunk_v1",
  "vector": [0.01, 0.02, 0.03, 0.04],
  "topK": 5,
  "outputFields": ["chunk_id", "title", "content"],
  "filterObject": {
    "tenant_id": 1001
  }
}
```

注意：真实 `vector` 长度必须等于 Collection 定义的 `dimension`。

### 1.4 删除数据

按主键删除：

```http
POST http://127.0.0.1:18080/api/vector/documents/delete
Content-Type: application/json
```

```json
{
  "collection": "knowledge_chunk_v1",
  "ids": ["doc_001_chunk_001", "doc_002_chunk_001"]
}
```

按条件删除：

```json
{
  "collection": "knowledge_chunk_v1",
  "filterObject": {
    "tenant_id": 1001,
    "business_code": "faq"
  }
}
```

按复杂条件删除：

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

注意：按条件删除时，`filterObject` 或 `filters` 不能为空，避免误删整个 Collection。如果同时传 `ids` 和条件，接口会优先按 `ids` 删除。

## 2. Java Domain 模式

适合已经引入 Maven SDK，并且服务 classpath 中存在对应 domain 类的用户。

### 2.1 写入或更新数据

```json
{
  "domain": "com.example.KnowledgeChunk",
  "items": [
    {
      "chunkId": "doc_001_chunk_001",
      "tenantId": 1001,
      "bizId": "doc-001",
      "businessCode": "faq",
      "title": "发票申请说明",
      "content": "登录系统后进入订单页面，选择需要开票的订单并提交发票信息。"
    }
  ]
}
```

### 2.2 向量搜索

```json
{
  "domain": "com.example.KnowledgeChunk",
  "query": "如何申请发票",
  "topK": 5,
  "minScore": 0.2,
  "outputFields": ["chunkId", "tenantId", "bizId", "businessCode", "title", "content"],
  "filterObject": {
    "tenantId": 1001,
    "businessCode": "faq"
  }
}
```

### 2.3 删除数据

按主键删除：

```json
{
  "domain": "com.example.KnowledgeChunk",
  "ids": ["doc_001_chunk_001"]
}
```

按条件删除：

```json
{
  "domain": "com.example.KnowledgeChunk",
  "filterObject": {
    "tenantId": 1001,
    "businessCode": "faq"
  }
}
```
