package com.rongqi.vector.milvus;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.rongqi.vector.core.VectorErrorCode;
import com.rongqi.vector.core.VectorException;
import com.rongqi.vector.core.schema.VectorCollectionDefinition;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;

/**
 * 基于 JDBC 的 Collection schema 存储实现。
 *
 * <p>该实现适合生产和多实例部署，多个服务实例可以共享同一张 schema 表。</p>
 */
public class JdbcVectorCollectionDefinitionStore implements VectorCollectionDefinitionStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final DataSource dataSource;
    private final String tableName;

    /**
     * 创建 JDBC 存储实现。
     *
     * @param dataSource 数据源
     * @param tableName schema 表名
     * @param initializeSchema 是否自动创建表
     */
    public JdbcVectorCollectionDefinitionStore(DataSource dataSource, String tableName, boolean initializeSchema) {
        if (dataSource == null) {
            throw new VectorException(VectorErrorCode.VECTOR_CONFIG_INVALID,
                    "JDBC schema 存储需要提供 DataSource");
        }
        this.dataSource = dataSource;
        this.tableName = resolveTableName(tableName);
        if (initializeSchema) {
            initializeSchema();
        }
    }

    @Override
    public List<VectorCollectionDefinition> loadAll() {
        List<VectorCollectionDefinition> definitions = new ArrayList<>();
        String sql = "select schema_json from " + tableName;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                VectorCollectionDefinition definition = GSON.fromJson(
                        resultSet.getString("schema_json"),
                        VectorCollectionDefinition.class);
                if (definition != null) {
                    definitions.add(definition);
                }
            }
            return definitions;
        } catch (SQLException exception) {
            throw new VectorException(VectorErrorCode.VECTOR_CONFIG_INVALID,
                    "加载 JDBC collection schema 失败: " + tableName, exception);
        }
    }

    @Override
    public void save(VectorCollectionDefinition definition) {
        String schemaJson = GSON.toJson(definition);
        Timestamp now = Timestamp.from(Instant.now());
        if (update(definition.getCollection(), schemaJson, now) == 0) {
            insert(definition.getCollection(), schemaJson, now);
        }
    }

    private void initializeSchema() {
        try (Connection connection = dataSource.getConnection()) {
            String productName = connection.getMetaData().getDatabaseProductName();
            String sql = buildCreateTableSql(productName);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.executeUpdate();
            }
            applySchemaComments(connection, productName);
        } catch (SQLException exception) {
            throw new VectorException(VectorErrorCode.VECTOR_CONFIG_INVALID,
                    "初始化 JDBC collection schema 表失败: " + tableName, exception);
        }
    }

    /**
     * 根据数据库类型生成建表 SQL。
     *
     * <p>MySQL/MariaDB 支持在建表语句中直接声明字段和表注释，因此直接把 comment 放进 SQL。</p>
     */
    private String buildCreateTableSql(String productName) {
        if (isMysqlLike(productName)) {
            return "create table if not exists " + tableName + " ("
                    + "collection_name varchar(255) primary key comment 'Collection 名称，作为 schema 主键', "
                    + "schema_json text not null comment 'Collection schema JSON 内容', "
                    + "updated_at timestamp not null comment 'schema 更新时间'"
                    + ") comment = 'RongQi Vector HTTP Collection schema 注册表'";
        }
        return "create table if not exists " + tableName + " ("
                + "collection_name varchar(255) primary key, "
                + "schema_json text not null, "
                + "updated_at timestamp not null"
                + ")";
    }

    /**
     * 判断当前数据库是否使用 MySQL/MariaDB 风格的建表注释语法。
     */
    private boolean isMysqlLike(String productName) {
        String normalizedProductName = normalizeProductName(productName);
        return normalizedProductName.contains("mysql") || normalizedProductName.contains("mariadb");
    }

    /**
     * 统一转换数据库产品名称，避免空值判断散落在调用处。
     */
    private String normalizeProductName(String productName) {
        return productName == null ? "" : productName.toLowerCase();
    }

    /**
     * 判断当前数据库是否支持 COMMENT ON 风格注释语法。
     */
    private boolean supportsCommentOn(String productName) {
        String normalizedProductName = normalizeProductName(productName);
        return normalizedProductName.contains("postgresql")
                || normalizedProductName.contains("h2")
                || normalizedProductName.contains("oracle");
    }

    /**
     * 执行一条 SQL。
     */
    private void executeSql(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        }
    }

    /**
     * 更新已存在的 schema 记录。
     */
    private int update(String collection, String schemaJson, Timestamp updatedAt) {
        String sql = "update " + tableName + " set schema_json = ?, updated_at = ? where collection_name = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, schemaJson);
            statement.setTimestamp(2, updatedAt);
            statement.setString(3, collection);
            return statement.executeUpdate();
        } catch (SQLException exception) {
            throw new VectorException(VectorErrorCode.VECTOR_CONFIG_INVALID,
                    "更新 JDBC collection schema 失败: " + collection, exception);
        }
    }

    /**
     * 插入新的 schema 记录。
     */
    private void insert(String collection, String schemaJson, Timestamp updatedAt) {
        String sql = "insert into " + tableName
                + " (collection_name, schema_json, updated_at) values (?, ?, ?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, collection);
            statement.setString(2, schemaJson);
            statement.setTimestamp(3, updatedAt);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new VectorException(VectorErrorCode.VECTOR_CONFIG_INVALID,
                    "保存 JDBC collection schema 失败: " + collection, exception);
        }
    }

    /**
     * 判断字符串是否包含有效文本。
     */
    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * 解析并校验 schema 表名，避免表名拼接 SQL 时出现非法字符。
     */
    private String resolveTableName(String configuredTableName) {
        String actualTableName = hasText(configuredTableName)
                ? configuredTableName.trim()
                : "vector_collection_schema";
        if (!actualTableName.matches("[A-Za-z0-9_]+")) {
            throw new VectorException(VectorErrorCode.VECTOR_CONFIG_INVALID,
                    "JDBC schema 表名只能包含字母、数字和下划线: " + actualTableName);
        }
        return actualTableName;
    }

    /**
     * 为 schema 表和字段添加数据库注释。
     *
     * <p>不同数据库的注释语法不一致，这里按数据库产品名称做少量适配。</p>
     */
    private void applySchemaComments(Connection connection, String productName) throws SQLException {
        if (supportsCommentOn(productName)) {
            applyStandardComments(connection);
        }
    }

    /**
     * 使用 COMMENT ON 标准风格语法添加表和字段注释。
     */
    private void applyStandardComments(Connection connection) throws SQLException {
        executeSql(connection, "comment on table " + tableName
                + " is 'RongQi Vector HTTP Collection schema 注册表'");
        executeSql(connection, "comment on column " + tableName
                + ".collection_name is 'Collection 名称，作为 schema 主键'");
        executeSql(connection, "comment on column " + tableName
                + ".schema_json is 'Collection schema JSON 内容'");
        executeSql(connection, "comment on column " + tableName
                + ".updated_at is 'schema 更新时间'");
    }
}
