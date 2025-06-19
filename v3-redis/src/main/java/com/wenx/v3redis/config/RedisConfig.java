package com.wenx.v3redis.config;

import com.wenx.v3redis.consts.RedisGeneratorConst;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scripting.ScriptSource;
import org.springframework.scripting.support.ResourceScriptSource;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Random;


/**
 * @author wenx
 * @description redis 配置
 */
@Configuration
@EnableCaching
@Slf4j
public class RedisConfig {

    /**
     * 通用的RedisTemplate配置
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        template.setEnableTransactionSupport(true);
        
        // 使用StringRedisSerializer来序列化和反序列化redis的key值
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringRedisSerializer);
        template.setHashKeySerializer(stringRedisSerializer);
        
        // 使用Jackson2JsonRedisSerializer来序列化和反序列化redis的value值
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = createJacksonSerializer();
        template.setValueSerializer(jackson2JsonRedisSerializer);
        template.setHashValueSerializer(jackson2JsonRedisSerializer);
        
        template.afterPropertiesSet();
        return template;
    }

    /**
     * 专门用于Hash操作的RedisTemplate
     */
    @Bean("hashRedisTemplate")
    public RedisTemplate<String, Object> hashRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        template.setEnableTransactionSupport(true);
        
        // 设置key的序列化方式
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringRedisSerializer);
        
        // 设置hash key的序列化方式
        template.setHashKeySerializer(stringRedisSerializer);
        
        // 设置value的序列化方式
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = createJacksonSerializer();
        template.setValueSerializer(jackson2JsonRedisSerializer);
        
        // 设置hash value的序列化方式
        template.setHashValueSerializer(jackson2JsonRedisSerializer);
        
        template.afterPropertiesSet();
        return template;
    }

    /**
     * 创建Jackson序列化器
     */
    private Jackson2JsonRedisSerializer<Object> createJacksonSerializer() {
        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        om.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);
        return new Jackson2JsonRedisSerializer<>(om, Object.class);
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) throws NoSuchAlgorithmException {
        RedisSerializer<String> redisSerializer = new StringRedisSerializer();
        // 解决查询缓存转换异常的问题
        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);

        /**
         * 新版本中om.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL)已经被废弃
         * 建议替换为om.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL)
         */
        om.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);
        Jackson2JsonRedisSerializer<?> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(om, Object.class);

        Random rand = SecureRandom.getInstanceStrong();
        // 配置序列化解决乱码的问题
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                // 设置缓存过期时间  为解决缓存雪崩,所以将过期时间加随机值
                .entryTtl(Duration.ofSeconds(60 * 60 + rand.nextInt(60 * 10)))
                // 设置key的序列化方式
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(redisSerializer))
                // 设置value的序列化方式
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jackson2JsonRedisSerializer));
        // .disableCachingNullValues(); //为防止缓存击穿，所以允许缓存null值
        return RedisCacheManager.builder(factory)
                .cacheDefaults(config)
                // 启用RedisCache以将缓存 put/evict 操作与正在进行的 Spring 管理的事务同步
                .transactionAware()
                .build();
    }

    @Bean(RedisGeneratorConst.GENERATOR_NAME)
    public KeyGenerator keyGenerator() {
        return (target, method, params) -> {
            StringBuilder sb = new StringBuilder();
            // 类名简化，只取最后的类名，不要包名
            String className = target.getClass().getSimpleName();
            sb.append(className).append(":");
            // 方法名
            sb.append(method.getName());
            // 参数
            if (params.length > 0) {
                sb.append(":");
                for (int i = 0; i < params.length; i++) {
                    if (i > 0) {
                        sb.append("_");
                    }
                    if (params[i] == null) {
                        sb.append("null");
                    } else if (params[i] instanceof String || params[i] instanceof Number || params[i] instanceof Boolean) {
                        // 基本类型直接拼接
                        sb.append(params[i]);
                    } else {
                        // 复杂对象使用hashCode
                        sb.append(params[i].hashCode());
                    }
                }
            }
            String key = sb.toString();
            log.debug("Generated cache key: {}", key);
            return key;
        };
    }


    @Bean
    public RedisScript<Boolean> script() throws IOException {
        ScriptSource scriptSource = new ResourceScriptSource(new ClassPathResource("lua/checkandset.lua"));
        return RedisScript.of(scriptSource.getScriptAsString(), Boolean.class);
    }
}
