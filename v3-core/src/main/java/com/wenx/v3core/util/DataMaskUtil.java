package com.wenx.v3core.util;

import com.wenx.v3core.anoo.DataMask;
import com.wenx.v3core.anoo.DataMaskProcessor;

/**
 * 数据脱敏工具类
 * 
 * <p>提供常用的数据脱敏方法，支持手动调用</p>
 * 
 * @author wenx
 */
public class DataMaskUtil {

    /**
     * 手机号脱敏
     * 
     * @param phone 手机号
     * @return 脱敏后的手机号
     */
    public static String maskPhone(String phone) {
        return DataMaskProcessor.maskData(phone, createAnnotation(DataMask.MaskStrategy.PHONE));
    }

    /**
     * 身份证号脱敏
     * 
     * @param idCard 身份证号
     * @return 脱敏后的身份证号
     */
    public static String maskIdCard(String idCard) {
        return DataMaskProcessor.maskData(idCard, createAnnotation(DataMask.MaskStrategy.ID_CARD));
    }

    /**
     * 邮箱脱敏
     * 
     * @param email 邮箱
     * @return 脱敏后的邮箱
     */
    public static String maskEmail(String email) {
        return DataMaskProcessor.maskData(email, createAnnotation(DataMask.MaskStrategy.EMAIL));
    }

    /**
     * 银行卡号脱敏
     * 
     * @param bankCard 银行卡号
     * @return 脱敏后的银行卡号
     */
    public static String maskBankCard(String bankCard) {
        return DataMaskProcessor.maskData(bankCard, createAnnotation(DataMask.MaskStrategy.BANK_CARD));
    }

    /**
     * 姓名脱敏
     * 
     * @param name 姓名
     * @return 脱敏后的姓名
     */
    public static String maskName(String name) {
        return DataMaskProcessor.maskData(name, createAnnotation(DataMask.MaskStrategy.NAME));
    }

    /**
     * 地址脱敏
     * 
     * @param address 地址
     * @return 脱敏后的地址
     */
    public static String maskAddress(String address) {
        return DataMaskProcessor.maskData(address, createAnnotation(DataMask.MaskStrategy.ADDRESS));
    }

    /**
     * 密码脱敏
     * 
     * @param password 密码
     * @return 脱敏后的密码
     */
    public static String maskPassword(String password) {
        return DataMaskProcessor.maskData(password, createAnnotation(DataMask.MaskStrategy.PASSWORD));
    }

    /**
     * IP地址脱敏
     * 
     * @param ip IP地址
     * @return 脱敏后的IP地址
     */
    public static String maskIpAddress(String ip) {
        return DataMaskProcessor.maskData(ip, createAnnotation(DataMask.MaskStrategy.IP_ADDRESS));
    }

    /**
     * 自定义脱敏
     * 
     * @param data 原始数据
     * @param prefixKeep 保留前几位
     * @param suffixKeep 保留后几位
     * @param maskChar 脱敏字符
     * @return 脱敏后的数据
     */
    public static String maskCustom(String data, int prefixKeep, int suffixKeep, char maskChar) {
        return DataMaskProcessor.maskData(data, createCustomAnnotation(prefixKeep, suffixKeep, maskChar));
    }

    /**
     * 自定义脱敏（使用默认*字符）
     * 
     * @param data 原始数据
     * @param prefixKeep 保留前几位
     * @param suffixKeep 保留后几位
     * @return 脱敏后的数据
     */
    public static String maskCustom(String data, int prefixKeep, int suffixKeep) {
        return maskCustom(data, prefixKeep, suffixKeep, '*');
    }

    /**
     * 智能脱敏 - 自动识别数据类型并应用相应的脱敏策略
     * 
     * @param data 原始数据
     * @return 脱敏后的数据
     */
    public static String maskSmart(String data) {
        if (isBlank(data)) {
            return data;
        }

        // 尝试识别数据类型
        if (DataMaskProcessor.validateFormat(data, DataMask.MaskStrategy.PHONE)) {
            return maskPhone(data);
        } else if (DataMaskProcessor.validateFormat(data, DataMask.MaskStrategy.ID_CARD)) {
            return maskIdCard(data);
        } else if (DataMaskProcessor.validateFormat(data, DataMask.MaskStrategy.EMAIL)) {
            return maskEmail(data);
        } else if (DataMaskProcessor.validateFormat(data, DataMask.MaskStrategy.BANK_CARD)) {
            return maskBankCard(data);
        } else if (DataMaskProcessor.validateFormat(data, DataMask.MaskStrategy.IP_ADDRESS)) {
            return maskIpAddress(data);
        } else {
            // 默认使用中间脱敏
            return maskCustom(data, 2, 2);
        }
    }

    /**
     * 批量脱敏
     * 
     * @param data 数据数组
     * @param strategy 脱敏策略
     * @return 脱敏后的数据数组
     */
    public static String[] maskBatch(String[] data, DataMask.MaskStrategy strategy) {
        if (data == null || data.length == 0) {
            return data;
        }

        String[] result = new String[data.length];
        DataMask annotation = createAnnotation(strategy);
        
        for (int i = 0; i < data.length; i++) {
            result[i] = DataMaskProcessor.maskData(data[i], annotation);
        }
        
        return result;
    }

    /**
     * 验证数据格式
     * 
     * @param data 数据
     * @param strategy 策略
     * @return 是否匹配格式
     */
    public static boolean validateFormat(String data, DataMask.MaskStrategy strategy) {
        return DataMaskProcessor.validateFormat(data, strategy);
    }

    /**
     * 创建默认的DataMask注解实例
     * 
     * @param strategy 脱敏策略
     * @return DataMask注解实例
     */
    private static DataMask createAnnotation(DataMask.MaskStrategy strategy) {
        return new DataMask() {
            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return DataMask.class;
            }

            @Override
            public MaskStrategy strategy() {
                return strategy;
            }

            @Override
            public String customRule() {
                return "";
            }

            @Override
            public int prefixKeep() {
                return 3;
            }

            @Override
            public int suffixKeep() {
                return 4;
            }

            @Override
            public char maskChar() {
                return '*';
            }

            @Override
            public boolean enabled() {
                return true;
            }
        };
    }

    /**
     * 创建自定义的DataMask注解实例
     * 
     * @param prefixKeep 保留前几位
     * @param suffixKeep 保留后几位
     * @param maskChar 脱敏字符
     * @return DataMask注解实例
     */
    private static DataMask createCustomAnnotation(int prefixKeep, int suffixKeep, char maskChar) {
        return new DataMask() {
            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return DataMask.class;
            }

            @Override
            public MaskStrategy strategy() {
                return MaskStrategy.CUSTOM;
            }

            @Override
            public String customRule() {
                return "";
            }

            @Override
            public int prefixKeep() {
                return prefixKeep;
            }

            @Override
            public int suffixKeep() {
                return suffixKeep;
            }

            @Override
            public char maskChar() {
                return maskChar;
            }

            @Override
            public boolean enabled() {
                return true;
            }
        };
    }

    /**
     * 判断字符串是否为空白
     */
    private static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }
} 