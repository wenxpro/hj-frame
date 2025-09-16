package com.wenx.v3core.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.wenx.v3core.enums.BaseEnum;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * 枚举Jackson配置
 * 提供全局的枚举序列化和反序列化支持
 */
@Configuration
public class EnumJacksonConfig {

    /**
     * BaseEnum序列化器
     */
    public static class BaseEnumSerializer extends StdSerializer<BaseEnum<?>> {
        
        public BaseEnumSerializer() {
            super(BaseEnum.class, false);
        }
        
        @Override
        public void serialize(BaseEnum<?> value, JsonGenerator gen, SerializerProvider provider) 
                throws IOException {
            if (value != null) {
                gen.writeObject(value.getValue());
            } else {
                gen.writeNull();
            }
        }
    }

    /**
     * 注册枚举序列化模块
     */
    @Bean
    public SimpleModule enumModule() {
        SimpleModule module = new SimpleModule("EnumModule");
        
        // 注册BaseEnum的序列化器
        module.addSerializer(new BaseEnumSerializer());
        
        return module;
    }
}