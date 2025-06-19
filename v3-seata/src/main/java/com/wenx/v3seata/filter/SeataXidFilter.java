package com.wenx.v3seata.filter;

import io.seata.core.context.RootContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Seata XID 过滤器
 * 用于接收并绑定分布式事务 XID
 * 
 * @author v3-cloud
 * @since 1.0
 */
@Slf4j
@Component
@Order(Integer.MIN_VALUE)
public class SeataXidFilter extends OncePerRequestFilter {
    
    /**
     * XID 请求头名称 - 使用 Seata 官方推荐的请求头
     */
    private static final String SEATA_XID_HEADER = RootContext.KEY_XID;
    
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, 
                                  @NonNull HttpServletResponse response, 
                                  @NonNull FilterChain filterChain) throws ServletException, IOException {
        
        var xid = request.getHeader(SEATA_XID_HEADER);
        var bind = false;
        
        try {
            if (StringUtils.hasText(xid)) {
                // 绑定 XID 到当前线程
                RootContext.bind(xid);
                bind = true;
                log.debug("Bind Seata XID from header: {}, URI: {}", xid, request.getRequestURI());
            }
            
            // 继续处理请求
            filterChain.doFilter(request, response);
            
        } finally {
            if (bind) {
                // 解绑 XID
                var unbindXid = RootContext.unbind();
                log.debug("Unbind Seata XID: {}, URI: {}", unbindXid, request.getRequestURI());
            }
        }
    }
}