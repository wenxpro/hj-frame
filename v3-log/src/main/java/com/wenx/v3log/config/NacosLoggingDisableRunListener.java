package com.wenx.v3log.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationRunListener;

/**
 * 提前禁用 Nacos 客户端自动注入的 logback 配置（读系统属性，Spring env 配置不生效）。
 *
 * <p>背景：v3 服务用 spring.config.import 加载 Nacos 配置时，NacosConfigDataLoader 会在
 * 环境准备阶段初始化 Nacos 客户端并应用其 logback 配置；随后 SCA 的 NacosLoggingAppRunListener
 * 在 environmentPrepared/contextPrepared 再次应用同一配置，把 nacos/config.log、naming.log、
 * remote.log 等 appender 重复加载，触发 logback "Collisions detected" 启动失败。
 *
 * <p>run listener 的构造函数在 SpringApplication.run 中最先执行（早于一切环境准备与配置导入），
 * 在这里设置系统属性可保证 Nacos 日志适配器构建时即读取到关闭开关。
 *
 * @author wenx
 */
public class NacosLoggingDisableRunListener implements SpringApplicationRunListener {

    private static final String NACOS_LOGGING_DEFAULT_CONFIG_ENABLED = "nacos.logging.default.config.enabled";

    public NacosLoggingDisableRunListener(SpringApplication application, String[] args) {
        System.setProperty(NACOS_LOGGING_DEFAULT_CONFIG_ENABLED, "false");
    }
}
