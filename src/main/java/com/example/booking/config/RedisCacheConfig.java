package com.example.booking.config;

import com.example.booking.controller.dto.OrdersDto;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;


@Configuration
public class RedisCacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {

        // Serializadores para diferentes tipos
        Jackson2JsonRedisSerializer<OrdersDto> ordersSerializer = new Jackson2JsonRedisSerializer<>(OrdersDto.class);
        // Jackson2JsonRedisSerializer<ProductDto> productSerializer = new Jackson2JsonRedisSerializer<>(ProductDto.class);

        // Configurações para cada tipo de cache
        RedisCacheConfiguration ordersCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(ordersSerializer));
        /*
        RedisCacheConfiguration productCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(20))
                .disableCachingNullValues()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(productSerializer));
        */
        // Mapeia cada nome de cache para sua configuração
        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        cacheConfigs.put("ORDERS_CACHE", ordersCacheConfig);
        // cacheConfigs.put("PRODUCT_CACHE", productCacheConfig);

        // Usa ORDERS_CACHE como default só se desejar, ou use um default genérico se preferir
        return RedisCacheManager.builder(redisConnectionFactory)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }
}
