package com.wenx.v3seata.interceptor;

import io.seata.core.context.RootContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Seata RestTemplate 拦截器
 * 用于在 RestTemplate 调用时传递分布式事务 XID
 * 
 * @author v3-cloud
 * @since 1.0
 */
@Slf4j
@Component
public class SeataRestTemplateInterceptor implements ClientHttpRequestInterceptor {
    
    /**
     * XID 请求头名称 - 使用 Seata 官方推荐的请求头
     */
    private static final String SEATA_XID_HEADER = RootContext.KEY_XID;
    
    @Override
    @NonNull
    public ClientHttpResponse intercept(@NonNull HttpRequest request, 
                                      @NonNull byte[] body, 
                                      @NonNull ClientHttpRequestExecution execution) throws IOException {
        var xid = RootContext.getXID();
        if (xid != null) {
            request.getHeaders().add(SEATA_XID_HEADER, xid);
            log.debug("Add Seata XID to RestTemplate request: {}, URL: {}", xid, request.getURI());
        }
        
        return execution.execute(request, body);
    }
} 