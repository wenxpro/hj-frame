package com.wenx.v3log.utl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.wenx.v3core.error.ServerExceptionEnum;
import com.wenx.v3core.error.ServiceException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import org.springframework.http.MediaType;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;

/**
 * @author wenx
 * @description HttpServlet 工具
 */
public class HttpServletUtil {

    public static HttpServletRequest getRequest() {
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (requestAttributes == null) {
            throw new ServiceException(ServerExceptionEnum.REQUEST_EMPTY);
        } else {
            return requestAttributes.getRequest();
        }
    }

    public static HttpServletResponse getResponse() {
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (requestAttributes == null) {
            throw new ServiceException(ServerExceptionEnum.REQUEST_EMPTY);
        } else {
            return requestAttributes.getResponse();
        }
    }

    /**
     * 客户端返回JSON字符串
     *
     * @param object
     * @return
     */
    public static void renderJson(Object object) {
        HttpServletResponse response = HttpServletUtil.getResponse();
        renderJson(response, JSON.toJSONString(object), MediaType.APPLICATION_JSON.toString());
    }

    /**
     * 客户端返回字符串
     *
     * @param response
     * @param string
     * @return
     */
    @SneakyThrows
    public static void renderJson(HttpServletResponse response, String string, String type) {
        response.setContentType(type);
        response.setCharacterEncoding(StandardCharsets.UTF_8.toString());
        response.getWriter().print(string);
        response.getWriter().flush();
        response.getWriter().close();
    }

    /**
     * 获取客户端IP
     */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) return "unknown";

        String ip = request.getHeader("X-Forwarded-For");
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    /**
     * 获取用户代理
     */
    public static String getUserAgent(HttpServletRequest request) {
        return request != null ? request.getHeader("User-Agent") : "unknown";
    }
}
