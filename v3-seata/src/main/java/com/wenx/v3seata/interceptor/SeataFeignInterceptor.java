package com.wenx.v3seata.interceptor;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import io.seata.core.context.RootContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Seata Feign 拦截器
 * 用于在 Feign 调用时传递分布式事务 XID
 * 
 * @author v3-cloud
 * @since 1.0
 */
@Slf4j
@Component
public class SeataFeignInterceptor implements RequestInterceptor {
    
    /**
     * XID 请求头名称 - 使用 Seata 官方推荐的请求头
     */
    private static final String SEATA_XID_HEADER = RootContext.KEY_XID;
    
    @Override
    public void apply(RequestTemplate template) {
        var xid = RootContext.getXID();
        if (xid != null) {
            template.header(SEATA_XID_HEADER, xid);
            log.debug("Add Seata XID to Feign request: {}, URL: {}", xid, template.url());
        }
    }
} 