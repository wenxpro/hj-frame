package com.wenx.v3redis.config;

import com.wenx.v3redis.consts.RedisGeneratorConst;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.Arrays;

import static com.wenx.v3redis.consts.RedisGeneratorConst.*;

/**
 * 自定义缓存Key生成器配置
 * 
 * @author wenx
 * @description 提供多种缓存key生成策略
 */
@Slf4j
@Configuration
public class CustomKeyGenerator {

    /**
     * 简单key生成器
     * 格式：ClassName:methodName:param1_param2
     */
    @Bean(SIMPLE_GENERATOR_NAME)
    public KeyGenerator simpleKeyGenerator() {
        return (target, method, params) -> {
            return new StringBuilder()
                    .append(target.getClass().getSimpleName())
                    .append(":")
                    .append(method.getName())
                    .append(":")
                    .append(StringUtils.arrayToDelimitedString(params, "_"))
                    .toString();
        };
    }

    /**
     * 带前缀的key生成器
     * 格式：prefix:ClassName:methodName:params
     */
    @Bean(PREFIX_GENERATOR_NAME)
    public KeyGenerator prefixKeyGenerator() {
        return new PrefixKeyGenerator("cache");
    }

    /**
     * 基于方法参数类型的key生成器
     * 格式：ClassName.methodName(paramType1,paramType2):param1_param2
     */
    @Bean(METHOD_GENERATOR_NAME)
    public KeyGenerator methodKeyGenerator() {
        return (target, method, params) -> {
            StringBuilder sb = new StringBuilder();
            // 类名和方法名
            sb.append(target.getClass().getSimpleName())
              .append(".")
              .append(method.getName())
              .append("(");
            
            // 参数类型
            Class<?>[] paramTypes = method.getParameterTypes();
            for (int i = 0; i < paramTypes.length; i++) {
                if (i > 0) sb.append(",");
                sb.append(paramTypes[i].getSimpleName());
            }
            sb.append(")");
            
            // 参数值
            if (params.length > 0) {
                sb.append(":");
                sb.append(StringUtils.arrayToDelimitedString(params, "_"));
            }
            
            return sb.toString();
        };
    }

    /**
     * 带前缀的key生成器实现
     */
    public static class PrefixKeyGenerator implements KeyGenerator {
        private final String prefix;

        public PrefixKeyGenerator(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Object generate(Object target, Method method, Object... params) {
            return new StringBuilder()
                    .append(prefix)
                    .append(":")
                    .append(target.getClass().getSimpleName())
                    .append(":")
                    .append(method.getName())
                    .append(":")
                    .append(Arrays.hashCode(params))
                    .toString();
        }
    }
} 