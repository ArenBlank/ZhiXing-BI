package com.huang.zhixing.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.RedisVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPooled;

@Configuration
public class VectorStoreConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6389}")
    private int redisPort;

    @Value("${spring.ai.vectorstore.redis.index:zhixing_bi_idx}")
    private String indexName;

    @Value("${spring.ai.vectorstore.redis.prefix:zhixing:doc:}")
    private String prefix;

    @Value("${spring.ai.vectorstore.redis.initialize-schema:true}")
    private boolean initializeSchema;

    @Bean
    public JedisPooled jedisPooled() {
        return new JedisPooled(redisHost, redisPort);
    }

    @Bean
    public VectorStore vectorStore(@Qualifier("ollamaEmbeddingModel") EmbeddingModel embeddingModel,
                                   JedisPooled jedisPooled) {
        RedisVectorStore.RedisVectorStoreConfig config =
                RedisVectorStore.RedisVectorStoreConfig.builder()
                        .withIndexName(indexName)
                        .withPrefix(prefix)
                        .withMetadataFields(
                                RedisVectorStore.MetadataField.tag("userId"),
                                RedisVectorStore.MetadataField.tag("sessionId"),
                                RedisVectorStore.MetadataField.tag("fileName")
                        )
                        .build();

        return new RedisVectorStore(config, embeddingModel, jedisPooled, initializeSchema);
    }
}
