package com.wenx.v3core.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.Map;

/**
 * @author wenx
 * http client util
 */
public class HttpUtil {
    /**
     * 发送post json 数据
     *
     * @param url
     * @param postData
     * @return
     */
    public static String postJson(String url, String postData) {
        HttpHeaders httpHeaders = new HttpHeaders();
        return postJson(url, postData, httpHeaders);
    }

    /**
     * 发送post json 数据
     *
     * @param url
     * @param postData
     * @param httpHeaders
     * @return
     */
    public static String postJson(String url, String postData, HttpHeaders httpHeaders) {
        String ret = null;
        try {
            RestTemplate client = new RestTemplate();
            MediaType type = MediaType.parseMediaType("application/json; charset=UTF-8");
            httpHeaders.setContentType(type);
            HttpEntity<String> httpEntity = new HttpEntity<>(postData, httpHeaders);
            ResponseEntity<String> response = client.exchange(url, HttpMethod.POST, httpEntity, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                ret = response.getBody();
            }
        } catch (Exception e) {
            //log.error("Exception:", e);
        }
        //log.debug(ret);
        return ret;
    }

    /**
     * 发送post表单数据，支持自定义headers
     *
     * @param url     请求URL
     * @param params  表单参数
     * @param headers 自定义请求头
     * @return 响应结果
     */
    public static String post(String url, Map<String, Object> params, Map<String, String> headers) {
        String ret = null;
        try {
            RestTemplate client = new RestTemplate();
            
            // 构建HttpHeaders
            HttpHeaders httpHeaders = new HttpHeaders();
            if (headers != null) {
                headers.forEach(httpHeaders::set);
            }
            
            // 设置Content-Type为表单格式（如果未设置）
            if (httpHeaders.getContentType() == null) {
                httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            }
            
            // 构建表单参数
            MultiValueMap<String, String> formParams = new LinkedMultiValueMap<>();
            if (params != null) {
                params.forEach((key, value) -> {
                    if (value != null) {
                        formParams.add(key, value.toString());
                    }
                });
            }
            
            HttpEntity<MultiValueMap<String, String>> httpEntity = new HttpEntity<>(formParams, httpHeaders);
            ResponseEntity<String> response = client.exchange(url, HttpMethod.POST, httpEntity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                ret = response.getBody();
            }
        } catch (Exception e) {
            //log.error("Exception:", e);
        }
        return ret;
    }

    /**
     * get 数据
     *
     * @param url
     * @param httpHeaders
     * @return
     */
    public static String get(String url, HttpHeaders httpHeaders) {
        String ret = null;
        try {
            RestTemplate client = new RestTemplate();
            HttpEntity<String> httpEntity = new HttpEntity<String>(null, httpHeaders);
            ResponseEntity<String> response = client.exchange(url, HttpMethod.GET, httpEntity, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                ret = response.getBody();
            }
        } catch (Exception e) {
            //log.error("Exception:", e);
        }
        //log.debug(ret);
        return ret;
    }

    /**
     * post数据
     *
     * @param url
     * @param httpHeaders
     * @return
     */
    public static String post(String url, HttpHeaders httpHeaders) {
        String ret = null;
        try {
            RestTemplate client = new RestTemplate();
            HttpEntity<String> httpEntity = new HttpEntity<String>(null, httpHeaders);
            ResponseEntity<String> response = client.exchange(url, HttpMethod.POST, httpEntity, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                ret = response.getBody();
            }
        } catch (Exception e) {
            //log.error("Exception:", e);
        }
        //log.debug(ret);
        return ret;
    }

    /**
     * new header post 方法
     *
     * @param url
     * @return
     */
    public static String post(String url) {
        String ret = null;
        try {
            RestTemplate client = new RestTemplate();
            HttpEntity<String> httpEntity = new HttpEntity<String>(null, new HttpHeaders());
            ResponseEntity<String> response = client.exchange(url, HttpMethod.POST, httpEntity, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                ret = response.getBody();
            }
        } catch (Exception e) {
            //log.error("Exception:", e);
        }
        //log.debug(ret);
        return ret;
    }


    /**
     * 获取cookie value
     *
     * @param req
     * @param name
     * @return
     */
    public static String getCookieValue(HttpServletRequest req, String name) {
        String value = null;
        Cookie[] cookies = req.getCookies();
        //如果浏览器中存在Cookie
        if (cookies != null && cookies.length > 0) {
            //遍历所有Cookie
            for (Cookie cookie : cookies) {
                //log.debug("cookie name={}",cookie.getName());
                if (cookie.getName().equals(name)) {
                    value = cookie.getValue();
                    //log.debug("cookie {}={}", name, value);
                    cookie.setMaxAge(0);
                }
            }
        }
        return value;
    }


    /**
     * 增加cookie
     *
     * @param resp
     * @param name
     * @param value
     * @param age
     * @return
     */
    public static boolean addCookie(HttpServletResponse resp, String name, String value, int age) {
        if (name == null || name.length() == 0) {
            return false;
        }
        if (value == null || value.length() == 0) {
            return false;
        }
        Cookie cookie = new Cookie(name, value);
        cookie.setMaxAge(age);
        cookie.setPath("/");
        resp.addCookie(cookie);
        return true;
    }

    /**
     * 增加cookie
     *
     * @param resp
     * @param name
     * @param value
     * @return
     */
    public static boolean addCookie(HttpServletResponse resp, String name, String value) {
        return addCookie(resp, name, value, -1);
    }


    /**
     * 判断是否是ajax请求
     *
     * @param httpRequest
     * @return
     */
    public static boolean isAjax(HttpServletRequest httpRequest) {
        String xRequestedWith = httpRequest.getHeader("X-Requested-With");
        return (xRequestedWith != null && "XMLHttpRequest".equals(xRequestedWith));
    }


    /**
     * 获取真实IP
     *
     * @param request
     * @return
     */
    public static String getClientIP(HttpServletRequest request) {
        String ip = request.getHeader("X-Real-IP");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Forwarded-For");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }


    /**
     * 获取请求参数
     *
     * @param req
     * @return
     */
    public static String getReqParam(HttpServletRequest req) {

        String optkey = "";
        //处理参数
        StringBuffer paramStr = new StringBuffer("{");
        try {
            Enumeration<String> params = req.getParameterNames();
            int n = 0;
            while (params.hasMoreElements()) {
                //得到参数名
                if (n != 0) {
                    paramStr.append(",");
                }
                n++;
                String name = params.nextElement();
                paramStr.append("\"" + name + "\"");
                paramStr.append(":");
                //得到参数对应值
                String[] value = req.getParameterValues(name);
                if (value == null) {
                    paramStr.append("\"\"");
                    continue;
                }
                //处理关键字
                if ("id".equalsIgnoreCase(name)) {
                    for (int i = 0; i < value.length; i++) {
                        if (i != 0) {
                            optkey += ",";
                        }
                        if (value[i] != null) {
                            if (value[i].indexOf("%") != -1) {
                                optkey += URLDecoder.decode(value[i], "utf-8");
                                continue;
                            }
                        }
                        optkey += value[i];
                    }
                }
                if (value.length > 1) {
                    paramStr.append("[");
                }
                for (int i = 0; i < value.length; i++) {
                    if (i != 0) {
                        paramStr.append(",");
                    }
                    if (value[i] != null) {
                        if (value[i].indexOf("%") != -1) {
                            paramStr.append(URLDecoder.decode("\"" + value[i], "utf-8") + "\"");
                            continue;
                        }
                    }
                    paramStr.append("\"" + value[i] + "\"");
                }
                if (value.length > 1) {
                    paramStr.append("]");
                }

            }
        } catch (Exception e) {
            //log.error("Exception:", e);

        }
        paramStr.append("}");
        return paramStr.toString();

    }
}