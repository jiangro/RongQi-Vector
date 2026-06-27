package com.rongqi.vector.spring;

import com.rongqi.vector.core.VectorTemplate;
import com.rongqi.vector.core.VectorErrorCode;
import com.rongqi.vector.core.VectorException;
import com.rongqi.vector.core.metadata.DomainMetadataParser;
import com.rongqi.vector.embedding.EmbeddingProvider;
import com.rongqi.vector.embedding.EmbeddingProviderRegistry;
import com.rongqi.vector.embedding.NoopEmbeddingProvider;
import com.rongqi.vector.embedding.http.HttpEmbeddingProperties;
import com.rongqi.vector.embedding.http.HttpEmbeddingProvider;
import com.rongqi.vector.milvus.FileVectorCollectionDefinitionStore;
import com.rongqi.vector.milvus.JdbcVectorCollectionDefinitionStore;
import com.rongqi.vector.milvus.MilvusClientProperties;
import com.rongqi.vector.milvus.MilvusGenericTemplate;
import com.rongqi.vector.milvus.MilvusVectorTemplate;
import com.rongqi.vector.milvus.VectorCollectionDefinitionRegistry;
import com.rongqi.vector.milvus.VectorCollectionDefinitionStore;
import com.rongqi.vector.rerank.bge.BgeRerankProvider;
import com.rongqi.vector.rerank.dashscope.DashScopeRerankProvider;
import com.rongqi.vector.rerank.http.HttpRerankProperties;
import com.rongqi.vector.core.rerank.RerankProvider;
import com.rongqi.vector.core.rerank.RerankProviderRegistry;
import java.nio.file.Paths;
import javax.sql.DataSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * RongQi Vector 自动配置。
 *
 * <p>Spring Boot 项目引入 starter 后，如果没有自定义 Bean，会自动创建
 * DomainMetadataParser、EmbeddingProviderRegistry 和 VectorTemplate。</p>
 */
@AutoConfiguration
@EnableConfigurationProperties(RongQiVectorProperties.class)
public class RongQiVectorAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public DomainMetadataParser domainMetadataParser() {
        return new DomainMetadataParser();
    }

    @Bean
    @ConditionalOnMissingBean
    public EmbeddingProviderRegistry embeddingProviderRegistry(RongQiVectorProperties properties,
                                                              ObjectProvider<EmbeddingProvider> providers) {
        EmbeddingProviderRegistry registry = new EmbeddingProviderRegistry();
        registry.register(new NoopEmbeddingProvider());
        if (properties.getHttpEmbedding().isEnabled()) {
            registry.register(new HttpEmbeddingProvider(toHttpEmbeddingProperties(properties.getHttpEmbedding())));
        }
        providers.orderedStream().forEach(registry::register);
        return registry;
    }

    @Bean
    @ConditionalOnMissingBean
    public RerankProviderRegistry rerankProviderRegistry(RongQiVectorProperties properties,
                                                        ObjectProvider<RerankProvider> providers) {
        RerankProviderRegistry registry = new RerankProviderRegistry();
        if (properties.getRerank().isEnabled()) {
            if (properties.getRerank().getDashscope().isEnabled()) {
                registry.register(new DashScopeRerankProvider(
                        toHttpRerankProperties(properties.getRerank(), properties.getRerank().getDashscope())));
            }
            if (properties.getRerank().getBge().isEnabled()) {
                registry.register(new BgeRerankProvider(
                        toHttpRerankProperties(properties.getRerank(), properties.getRerank().getBge())));
            }
        }
        providers.orderedStream().forEach(registry::register);
        return registry;
    }

    @Bean
    @ConditionalOnMissingBean
    public VectorTemplate vectorTemplate(RongQiVectorProperties properties,
                                         DomainMetadataParser metadataParser,
                                         EmbeddingProviderRegistry embeddingProviderRegistry,
                                         RerankProviderRegistry rerankProviderRegistry) {
        MilvusClientProperties milvus = new MilvusClientProperties();
        milvus.setUri(properties.getMilvus().getUri());
        milvus.setToken(properties.getMilvus().getToken());
        milvus.setDatabase(properties.getMilvus().getDatabase());
        return new MilvusVectorTemplate(
                milvus,
                metadataParser,
                embeddingProviderRegistry,
                properties.getEmbedding().getDefaultProvider(),
                properties.getEmbedding().getBatchSize(),
                rerankProviderRegistry,
                properties.getRerank().getDefaultProvider());
    }

    @Bean
    @ConditionalOnMissingBean
    public VectorCollectionDefinitionRegistry vectorCollectionDefinitionRegistry(
            RongQiVectorProperties properties,
            ObjectProvider<DataSource> dataSourceProvider) {
        return new VectorCollectionDefinitionRegistry(
                vectorCollectionDefinitionStore(properties, dataSourceProvider));
    }

    @Bean
    @ConditionalOnMissingBean
    public MilvusGenericTemplate milvusGenericTemplate(RongQiVectorProperties properties,
                                                       EmbeddingProviderRegistry embeddingProviderRegistry,
                                                       RerankProviderRegistry rerankProviderRegistry,
                                                       VectorCollectionDefinitionRegistry definitionRegistry) {
        MilvusClientProperties milvus = new MilvusClientProperties();
        milvus.setUri(properties.getMilvus().getUri());
        milvus.setToken(properties.getMilvus().getToken());
        milvus.setDatabase(properties.getMilvus().getDatabase());
        return new MilvusGenericTemplate(
                milvus,
                embeddingProviderRegistry,
                properties.getEmbedding().getDefaultProvider(),
                definitionRegistry,
                properties.getEmbedding().getBatchSize(),
                rerankProviderRegistry,
                properties.getRerank().getDefaultProvider());
    }

    private HttpEmbeddingProperties toHttpEmbeddingProperties(RongQiVectorProperties.HttpEmbedding source) {
        HttpEmbeddingProperties target = new HttpEmbeddingProperties();
        target.setName(source.getName());
        target.setUrl(source.getUrl());
        target.setApiKey(source.getApiKey());
        target.setModel(source.getModel());
        target.setDimension(source.getDimension());
        target.setTimeoutMillis(source.getTimeoutMillis());
        target.setMaxRetries(source.getMaxRetries());
        target.setRetryIntervalMillis(source.getRetryIntervalMillis());
        return target;
    }

    private HttpRerankProperties toHttpRerankProperties(RongQiVectorProperties.Rerank common,
                                                        RongQiVectorProperties.HttpRerank source) {
        HttpRerankProperties target = new HttpRerankProperties();
        target.setName(source.getName());
        target.setUrl(source.getUrl());
        target.setApiKey(source.getApiKey());
        target.setModel(source.getModel());
        target.setTimeoutMillis(common.getTimeoutMillis());
        target.setMaxRetries(common.getMaxRetries());
        target.setRetryIntervalMillis(common.getRetryIntervalMillis());
        return target;
    }

    /**
     * 根据配置创建 Collection schema 存储实现。
     *
     * <p>默认使用本地文件；显式配置为 jdbc 时使用 Spring 容器中的 DataSource。</p>
     */
    private VectorCollectionDefinitionStore vectorCollectionDefinitionStore(
            RongQiVectorProperties properties,
            ObjectProvider<DataSource> dataSourceProvider) {
        String type = properties.getSchema().getType();
        if (type == null || type.trim().isEmpty() || "file".equalsIgnoreCase(type)) {
            return new FileVectorCollectionDefinitionStore(Paths.get(properties.getSchema().getStorageDir()));
        }
        if ("jdbc".equalsIgnoreCase(type)) {
            DataSource dataSource = dataSourceProvider.getIfAvailable();
            if (dataSource == null) {
                throw new VectorException(VectorErrorCode.VECTOR_CONFIG_INVALID,
                        "rongqi.vector.schema.type=jdbc 时必须提供 DataSource");
            }
            return new JdbcVectorCollectionDefinitionStore(
                    dataSource,
                    properties.getSchema().getJdbc().getTableName(),
                    properties.getSchema().getJdbc().isInitializeSchema());
        }
        throw new VectorException(VectorErrorCode.VECTOR_CONFIG_INVALID,
                "不支持的 schema 存储类型: " + type + "，可选值为 file 或 jdbc");
    }
}
