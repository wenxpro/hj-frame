package com.wenx.v3redis.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lua脚本加载器
 * 负责加载和缓存Redis Lua脚本
 * 
 * @author wenx
 */
@Slf4j
@Component
public class LuaScriptLoader {
    
    // 脚本缓存
    private final Map<String, String> scriptCache = new ConcurrentHashMap<>();
    
    // 锁相关脚本路径
    private static final String LOCK_SCRIPT_PATH = "lua/lock/";
    
    @PostConstruct
    public void init() {
        // 预加载常用脚本
        try {
            // 分布式锁脚本
            loadScript("distributed_lock");
            loadScript("distributed_unlock");
            loadScript("distributed_renew");
            
            // 公平锁脚本
            loadScript("fair_lock");
            loadScript("fair_unlock");
            
            // 可重入锁脚本
            loadScript("reentrant_lock");
            loadScript("reentrant_unlock");
            
            // 读写锁脚本
            loadScript("read_lock");
            loadScript("write_lock");
            
            log.info("Lua脚本预加载完成，共加载 {} 个脚本", scriptCache.size());
        } catch (Exception e) {
            log.error("Lua脚本预加载失败", e);
        }
    }
    
    /**
     * 获取脚本内容
     * 
     * @param scriptName 脚本名称（不含扩展名）
     * @return 脚本内容
     */
    public String getScript(String scriptName) {
        return scriptCache.computeIfAbsent(scriptName, this::loadScriptFromFile);
    }
    
    /**
     * 获取Redis脚本对象
     * 
     * @param scriptName 脚本名称
     * @param resultType 返回值类型
     * @param <T> 返回值类型
     * @return Redis脚本对象
     */
    public <T> DefaultRedisScript<T> getRedisScript(String scriptName, Class<T> resultType) {
        String script = getScript(scriptName);
        DefaultRedisScript<T> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(script);
        redisScript.setResultType(resultType);
        return redisScript;
    }
    
    /**
     * 加载脚本
     * 
     * @param scriptName 脚本名称
     */
    private void loadScript(String scriptName) {
        String script = loadScriptFromFile(scriptName);
        if (script != null) {
            scriptCache.put(scriptName, script);
            log.debug("加载Lua脚本成功：{}", scriptName);
        }
    }
    
    /**
     * 从文件加载脚本
     * 
     * @param scriptName 脚本名称
     * @return 脚本内容
     */
    private String loadScriptFromFile(String scriptName) {
        try {
            String path = LOCK_SCRIPT_PATH + scriptName + ".lua";
            ClassPathResource resource = new ClassPathResource(path);
            
            if (!resource.exists()) {
                log.warn("Lua脚本文件不存在：{}", path);
                return null;
            }
            
            try (InputStreamReader reader = new InputStreamReader(
                    resource.getInputStream(), StandardCharsets.UTF_8)) {
                return FileCopyUtils.copyToString(reader);
            }
        } catch (IOException e) {
            log.error("加载Lua脚本失败：{}", scriptName, e);
            return null;
        }
    }
    
    /**
     * 清除脚本缓存
     */
    public void clearCache() {
        scriptCache.clear();
        log.info("Lua脚本缓存已清除");
    }
    
    /**
     * 重新加载所有脚本
     */
    public void reloadAll() {
        clearCache();
        init();
    }
} 