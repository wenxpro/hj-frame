package com.wenx.v3core.anoo;

import java.util.regex.Pattern;

/**
 * 数据脱敏处理器
 * 
 * <p>根据不同的脱敏策略对数据进行脱敏处理</p>
 * 
 * @author wenx
 */
public class DataMaskProcessor {

    /**
     * 手机号正则表达式
     */
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    
    /**
     * 身份证号正则表达式
     */
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("^\\d{17}[\\dX]$");
    
    /**
     * 邮箱正则表达式
     */
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");
    
    /**
     * 银行卡号正则表达式
     */
    private static final Pattern BANK_CARD_PATTERN = Pattern.compile("^\\d{16,19}$");
    
    /**
     * IP地址正则表达式
     */
    private static final Pattern IP_PATTERN = Pattern.compile("^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$");

    /**
     * 执行数据脱敏
     * 
     * @param data 原始数据
     * @param annotation 脱敏注解
     * @return 脱敏后的数据
     */
    public static String maskData(String data, DataMask annotation) {
        if (isBlank(data) || annotation == null || !annotation.enabled()) {
            return data;
        }

        try {
            return switch (annotation.strategy()) {
                case PHONE -> maskPhone(data, annotation);
                case ID_CARD -> maskIdCard(data, annotation);
                case EMAIL -> maskEmail(data, annotation);
                case BANK_CARD -> maskBankCard(data, annotation);
                case NAME -> maskName(data, annotation);
                case ADDRESS -> maskAddress(data, annotation);
                case PASSWORD -> maskPassword(data, annotation);
                case IP_ADDRESS -> maskIpAddress(data, annotation);
                case CUSTOM -> maskCustom(data, annotation);
                case NONE -> data;
            };
        } catch (Exception e) {
            System.err.println("数据脱敏处理失败: " + e.getMessage());
            return data;
        }
    }

    /**
     * 手机号脱敏：138****1234
     */
    private static String maskPhone(String phone, DataMask annotation) {
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            return phone;
        }
        
        int prefixKeep = Math.min(annotation.prefixKeep(), 3);
        int suffixKeep = Math.min(annotation.suffixKeep(), 4);
        
        return maskMiddle(phone, prefixKeep, suffixKeep, annotation.maskChar());
    }

    /**
     * 身份证号脱敏：110101********1234
     */
    private static String maskIdCard(String idCard, DataMask annotation) {
        if (!ID_CARD_PATTERN.matcher(idCard).matches()) {
            return idCard;
        }
        
        int prefixKeep = Math.min(annotation.prefixKeep(), 6);
        int suffixKeep = Math.min(annotation.suffixKeep(), 4);
        
        return maskMiddle(idCard, prefixKeep, suffixKeep, annotation.maskChar());
    }

    /**
     * 邮箱脱敏：abc***@example.com
     */
    private static String maskEmail(String email, DataMask annotation) {
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return email;
        }
        
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return email;
        }
        
        String username = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        
        if (username.length() <= 3) {
            return username.charAt(0) + repeat(String.valueOf(annotation.maskChar()), username.length() - 1) + domain;
        } else {
            return username.substring(0, 3) + repeat(String.valueOf(annotation.maskChar()), username.length() - 3) + domain;
        }
    }

    /**
     * 银行卡号脱敏：6222 **** **** 1234
     */
    private static String maskBankCard(String bankCard, DataMask annotation) {
        if (!BANK_CARD_PATTERN.matcher(bankCard).matches()) {
            return bankCard;
        }
        
        // 格式化为4位一组
        StringBuilder formatted = new StringBuilder();
        for (int i = 0; i < bankCard.length(); i += 4) {
            if (i > 0) {
                formatted.append(" ");
            }
            
            int end = Math.min(i + 4, bankCard.length());
            String group = bankCard.substring(i, end);
            
            // 保留前4位和后4位，中间用*代替
            if (i == 0) {
                formatted.append(group); // 前4位
            } else if (end == bankCard.length() && bankCard.length() - i <= 4) {
                formatted.append(group); // 后4位
            } else {
                formatted.append(repeat(String.valueOf(annotation.maskChar()), group.length()));
            }
        }
        
        return formatted.toString();
    }

    /**
     * 姓名脱敏：张*、李**
     */
    private static String maskName(String name, DataMask annotation) {
        if (name.length() <= 1) {
            return name;
        } else if (name.length() == 2) {
            return name.charAt(0) + String.valueOf(annotation.maskChar());
        } else {
            return name.charAt(0) + repeat(String.valueOf(annotation.maskChar()), name.length() - 1);
        }
    }

    /**
     * 地址脱敏：保留前6位，其余用*代替
     */
    private static String maskAddress(String address, DataMask annotation) {
        int prefixKeep = Math.min(annotation.prefixKeep(), 6);
        
        if (address.length() <= prefixKeep) {
            return address;
        }
        
        return address.substring(0, prefixKeep) + 
               repeat(String.valueOf(annotation.maskChar()), address.length() - prefixKeep);
    }

    /**
     * 密码脱敏：全部用*代替
     */
    private static String maskPassword(String password, DataMask annotation) {
        return repeat(String.valueOf(annotation.maskChar()), Math.min(password.length(), 8));
    }

    /**
     * IP地址脱敏：192.168.*.*
     */
    private static String maskIpAddress(String ip, DataMask annotation) {
        if (!IP_PATTERN.matcher(ip).matches()) {
            return ip;
        }
        
        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            return ip;
        }
        
        return parts[0] + "." + parts[1] + ".*.*";
    }

    /**
     * 自定义脱敏规则
     */
    private static String maskCustom(String data, DataMask annotation) {
        String customRule = annotation.customRule();
        if (isBlank(customRule)) {
            // 如果没有自定义规则，使用默认的中间脱敏
            return maskMiddle(data, annotation.prefixKeep(), annotation.suffixKeep(), annotation.maskChar());
        }
        
        try {
            // 支持正则表达式替换
            if (customRule.contains("->")) {
                String[] parts = customRule.split("->");
                if (parts.length == 2) {
                    return data.replaceAll(parts[0].trim(), parts[1].trim());
                }
            }
            
            // 默认处理
            return maskMiddle(data, annotation.prefixKeep(), annotation.suffixKeep(), annotation.maskChar());
        } catch (Exception e) {
            System.err.println("自定义脱敏规则执行失败: " + e.getMessage());
            return data;
        }
    }

    /**
     * 中间脱敏：保留前后指定位数，中间用指定字符代替
     * 
     * @param data 原始数据
     * @param prefixKeep 保留前几位
     * @param suffixKeep 保留后几位
     * @param maskChar 脱敏字符
     * @return 脱敏后的数据
     */
    private static String maskMiddle(String data, int prefixKeep, int suffixKeep, char maskChar) {
        if (data.length() <= prefixKeep + suffixKeep) {
            return data;
        }
        
        String prefix = data.substring(0, prefixKeep);
        String suffix = data.substring(data.length() - suffixKeep);
        int maskLength = data.length() - prefixKeep - suffixKeep;
        
        return prefix + repeat(String.valueOf(maskChar), maskLength) + suffix;
    }

    /**
     * 验证数据格式是否匹配指定策略
     * 
     * @param data 数据
     * @param strategy 策略
     * @return 是否匹配
     */
    public static boolean validateFormat(String data, DataMask.MaskStrategy strategy) {
        if (isBlank(data)) {
            return false;
        }
        
        return switch (strategy) {
            case PHONE -> PHONE_PATTERN.matcher(data).matches();
            case ID_CARD -> ID_CARD_PATTERN.matcher(data).matches();
            case EMAIL -> EMAIL_PATTERN.matcher(data).matches();
            case BANK_CARD -> BANK_CARD_PATTERN.matcher(data).matches();
            case IP_ADDRESS -> IP_PATTERN.matcher(data).matches();
            default -> true;
        };
    }

    /**
     * 判断字符串是否为空白
     */
    private static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * 重复字符串
     */
    private static String repeat(String str, int count) {
        if (count <= 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }
} 