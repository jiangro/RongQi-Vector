# RongQi Vector 开发规范守则

## 1. 命名

- 项目名：`RongQi Vector`
- Maven groupId：`com.rongqi.vector`
- Java 根包名：`com.rongqi.vector`
- Spring Boot 配置前缀：`rongqi.vector`
- 文档、代码、示例中不得出现旧项目或旧组织信息。

## 2. 易用性

RongQi Vector 面向普通业务开发者，API 必须简单、直观、少配置。

- 用户通过 domain 注解定义 Collection。
- 用户不需要在 YAML 中维护业务 schema。
- 用户不需要直接接触 Milvus SDK。
- 常用操作通过 `VectorTemplate` 或 Repository 完成。
- 错误信息必须告诉用户哪里错了、应该怎么改。

## 3. 注释

代码必须包含注释，尤其是对外 API。

- public class / interface / enum 必须写 JavaDoc。
- public method 必须写 JavaDoc。
- 自定义注解必须说明用途和示例。
- 配置属性必须说明默认值和影响。
- 复杂流程必须写注释，例如注解解析、filter 构建、embedding provider 选择。

## 4. Getter/Setter

业务 domain 不鼓励手写 getter/setter。

- 推荐使用 Lombok `@Getter`、`@Setter`、`@NoArgsConstructor`。
- 不建议直接使用 `@Data`，避免 `toString` 打印大文本或向量数组。
- 框架内部必须支持字段反射访问，不强制用户手写 getter/setter。
- Lambda 查询需要 getter 时，可以由 Lombok 自动生成。

## 5. Embedding

RongQi Vector 不能绑定单一 Embedding 模型。

- 核心逻辑只依赖 `EmbeddingProvider`。
- 内置 provider 和用户自定义 provider 使用同一接口。
- provider 名称必须唯一。
- provider 返回维度必须和向量字段注解一致。
- 用户直接传入向量时，不调用 embedding provider。

## 6. 测试

核心能力必须有测试覆盖。

- 注解解析。
- 类型自动推断。
- filter 构建。
- embedding provider 注册。
- Spring Boot 自动配置。
- Milvus 写入、搜索、删除集成测试。
