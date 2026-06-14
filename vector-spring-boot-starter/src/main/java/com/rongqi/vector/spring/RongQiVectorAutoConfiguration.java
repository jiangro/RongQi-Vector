package com.rongqi.vector.spring;

import com.rongqi.vector.core.VectorTemplate;
import com.rongqi.vector.core.metadata.DomainMetadataParser;
import com.rongqi.vector.embedding.EmbeddingProvider;
import com.rongqi.vector.embedding.EmbeddingProviderRegistry;
import com.rongqi.vector.embedding.NoopEmbeddingProvider;
import com.rongqi.vector.embedding.http.HttpEmbeddingProperties;
import com.rongqi.vector.embedding.http.HttpEmbeddingProvider;
import com.rongqi.vector.milvus.MilvusClientProperties;
import com.rongqi.vector.milvus.MilvusGenericTemplate;
import com.rongqi.vector.milvus.MilvusVectorTemplate;
import com.rongqi.vector.milvus.VectorCollectionDefinitionRegistry;
import java.nio.file.Paths;
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
    public VectorTemplate vectorTemplate(RongQiVectorProperties properties,
                                         DomainMetadataParser metadataParser,
                                         EmbeddingProviderRegistry embeddingProviderRegistry) {
        MilvusClientProperties milvus = new MilvusClientProperties();
        milvus.setUri(properties.getMilvus().getUri());
        milvus.setToken(properties.getMilvus().getToken());
        milvus.setDatabase(properties.getMilvus().getDatabase());
        return new MilvusVectorTemplate(
                milvus,
                metadataParser,
                embeddingProviderRegistry,
                properties.getEmbedding().getDefaultProvider());
    }

    @Bean
    @ConditionalOnMissingBean
    public VectorCollectionDefinitionRegistry vectorCollectionDefinitionRegistry(RongQiVectorProperties properties) {
        return new VectorCollectionDefinitionRegistry(Paths.get(properties.getSchema().getStorageDir()));
    }

    @Bean
    @ConditionalOnMissingBean
    public MilvusGenericTemplate milvusGenericTemplate(RongQiVectorProperties properties,
                                                       EmbeddingProviderRegistry embeddingProviderRegistry,
                                                       VectorCollectionDefinitionRegistry definitionRegistry) {
        MilvusClientProperties milvus = new MilvusClientProperties();
        milvus.setUri(properties.getMilvus().getUri());
        milvus.setToken(properties.getMilvus().getToken());
        milvus.setDatabase(properties.getMilvus().getDatabase());
        return new MilvusGenericTemplate(
                milvus,
                embeddingProviderRegistry,
                properties.getEmbedding().getDefaultProvider(),
                definitionRegistry);
    }

    private HttpEmbeddingProperties toHttpEmbeddingProperties(RongQiVectorProperties.HttpEmbedding source) {
        HttpEmbeddingProperties target = new HttpEmbeddingProperties();
        target.setName(source.getName());
        target.setUrl(source.getUrl());
        target.setApiKey(source.getApiKey());
        target.setModel(source.getModel());
        target.setDimension(source.getDimension());
        target.setTimeoutMillis(source.getTimeoutMillis());
        return target;
    }
}
