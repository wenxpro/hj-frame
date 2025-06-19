package com.wenx.v3core.util;

import cn.hutool.core.util.StrUtil;
import com.wenx.v3core.error.BusinessException;

import java.util.Collection;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 参数验证工具类
 * 
 * @author wenx
 */
public class ValidationUtil {
    
    /**
     * 手机号正则
     */
    private static final Pattern MOBILE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    
    /**
     * 邮箱正则
     */
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$");
    
    /**
     * 身份证号正则
     */
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("^[1-9]\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[\\dXx]$");
    
    /**
     * 验证非空
     */
    public static void notNull(Object object, String message) {
        if (object == null) {
            throw new BusinessException(message);
        }
    }
    
    /**
     * 验证非空字符串
     */
    public static void notBlank(String str, String message) {
        if (StrUtil.isBlank(str)) {
            throw new BusinessException(message);
        }
    }
    
    /**
     * 验证集合非空
     */
    public static void notEmpty(Collection<?> collection, String message) {
        if (collection == null || collection.isEmpty()) {
            throw new BusinessException(message);
        }
    }
    
    /**
     * 验证Map非空
     */
    public static void notEmpty(Map<?, ?> map, String message) {
        if (map == null || map.isEmpty()) {
            throw new BusinessException(message);
        }
    }
    
    /**
     * 验证数组非空
     */
    public static void notEmpty(Object[] array, String message) {
        if (array == null || array.length == 0) {
            throw new BusinessException(message);
        }
    }
    
    /**
     * 验证为真
     */
    public static void isTrue(boolean expression, String message) {
        if (!expression) {
            throw new BusinessException(message);
        }
    }
    
    /**
     * 验证为假
     */
    public static void isFalse(boolean expression, String message) {
        if (expression) {
            throw new BusinessException(message);
        }
    }
    
    /**
     * 验证手机号
     */
    public static void validMobile(String mobile, String message) {
        if (!isMobile(mobile)) {
            throw new BusinessException(message);
        }
    }
    
    /**
     * 验证邮箱
     */
    public static void validEmail(String email, String message) {
        if (!isEmail(email)) {
            throw new BusinessException(message);
        }
    }
    
    /**
     * 验证身份证号
     */
    public static void validIdCard(String idCard, String message) {
        if (!isIdCard(idCard)) {
            throw new BusinessException(message);
        }
    }
    
    /**
     * 是否为手机号
     */
    public static boolean isMobile(String mobile) {
        return StrUtil.isNotBlank(mobile) && MOBILE_PATTERN.matcher(mobile).matches();
    }
    
    /**
     * 是否为邮箱
     */
    public static boolean isEmail(String email) {
        return StrUtil.isNotBlank(email) && EMAIL_PATTERN.matcher(email).matches();
    }
    
    /**
     * 是否为身份证号
     */
    public static boolean isIdCard(String idCard) {
        return StrUtil.isNotBlank(idCard) && ID_CARD_PATTERN.matcher(idCard).matches();
    }
    
    /**
     * 验证长度范围
     */
    public static void lengthBetween(String str, int min, int max, String message) {
        if (str == null || str.length() < min || str.length() > max) {
            throw new BusinessException(message);
        }
    }
    
    /**
     * 验证数值范围
     */
    public static void numberBetween(Number number, Number min, Number max, String message) {
        if (number == null || number.doubleValue() < min.doubleValue() || number.doubleValue() > max.doubleValue()) {
            throw new BusinessException(message);
        }
    }
} 