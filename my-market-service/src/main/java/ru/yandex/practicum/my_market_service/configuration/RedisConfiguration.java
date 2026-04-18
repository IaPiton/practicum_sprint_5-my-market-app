package ru.yandex.practicum.my_market_service.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import ru.yandex.practicum.my_market_service.core.model.OrderDto;
import ru.yandex.practicum.my_market_service.persistence.entity.Item;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Configuration
@EnableCaching
public class RedisConfiguration {

    @Bean
    public RedisCacheManagerBuilderCustomizer itemCacheCustomizer() {
        ObjectMapper typedMapper = createTypedObjectMapper();
        ObjectMapper simpleMapper = createSimpleObjectMapper();

        GenericJackson2JsonRedisSerializer typedSerializer = new GenericJackson2JsonRedisSerializer(typedMapper);

        return builder -> builder
                .withCacheConfiguration("items", createItemCache(simpleMapper))
                .withCacheConfiguration("allItems", createCollectionCache(typedSerializer))
                .withCacheConfiguration("orders", createOrderCache(simpleMapper))
                .withCacheConfiguration("allOrders", createCollectionCache(typedSerializer));
    }

    private RedisCacheConfiguration createItemCache(ObjectMapper mapper) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new Jackson2JsonRedisSerializer<>(mapper, Item.class)));
    }

    private RedisCacheConfiguration createOrderCache(ObjectMapper mapper) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new Jackson2JsonRedisSerializer<>(mapper, OrderDto.class)));
    }

    private RedisCacheConfiguration createCollectionCache(GenericJackson2JsonRedisSerializer serializer) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.of(15, ChronoUnit.SECONDS))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));
    }

    private ObjectMapper createTypedObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.activateDefaultTyping(mapper.getPolymorphicTypeValidator(), ObjectMapper.DefaultTyping.NON_FINAL);
        return mapper;
    }

    private ObjectMapper createSimpleObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}