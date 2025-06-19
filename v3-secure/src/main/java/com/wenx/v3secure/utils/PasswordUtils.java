package com.wenx.v3secure.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.security.SecureRandom;
import java.util.Random;

/**
 * 密码工具类
 * 
 * @author wenx
 * @description 提供密码加密、验证等功能
 */
public class PasswordUtils {
    
    private static final PasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();
    
    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL = "!@#$%^&*()_+-=[]{}|;:,.<>?";
    
    /**
     * 加密密码
     * 
     * @param rawPassword 原始密码
     * @return 加密后的密码
     */
    public static String encode(String rawPassword) {
        return PASSWORD_ENCODER.encode(rawPassword);
    }
    
    /**
     * 验证密码
     * 
     * @param rawPassword 原始密码
     * @param encodedPassword 加密后的密码
     * @return 是否匹配
     */
    public static boolean matches(String rawPassword, String encodedPassword) {
        return PASSWORD_ENCODER.matches(rawPassword, encodedPassword);
    }
    
    /**
     * 生成随机密码
     * 
     * @param length 密码长度
     * @return 随机密码
     */
    public static String generateRandomPassword(int length) {
        return generateRandomPassword(length, true, true, true, true);
    }
    
    /**
     * 生成随机密码
     * 
     * @param length 密码长度
     * @param includeUpper 是否包含大写字母
     * @param includeLower 是否包含小写字母
     * @param includeDigits 是否包含数字
     * @param includeSpecial 是否包含特殊字符
     * @return 随机密码
     */
    public static String generateRandomPassword(int length, boolean includeUpper, 
                                                boolean includeLower, boolean includeDigits, 
                                                boolean includeSpecial) {
        if (length < 1) {
            throw new IllegalArgumentException("密码长度必须大于0");
        }
        
        StringBuilder chars = new StringBuilder();
        if (includeUpper) chars.append(UPPER);
        if (includeLower) chars.append(LOWER);
        if (includeDigits) chars.append(DIGITS);
        if (includeSpecial) chars.append(SPECIAL);
        
        if (chars.length() == 0) {
            throw new IllegalArgumentException("至少需要选择一种字符类型");
        }
        
        Random random = new SecureRandom();
        StringBuilder password = new StringBuilder(length);
        
        // 确保每种选中的字符类型至少包含一个
        if (includeUpper && length >= 1) {
            password.append(UPPER.charAt(random.nextInt(UPPER.length())));
        }
        if (includeLower && password.length() < length) {
            password.append(LOWER.charAt(random.nextInt(LOWER.length())));
        }
        if (includeDigits && password.length() < length) {
            password.append(DIGITS.charAt(random.nextInt(DIGITS.length())));
        }
        if (includeSpecial && password.length() < length) {
            password.append(SPECIAL.charAt(random.nextInt(SPECIAL.length())));
        }
        
        // 填充剩余长度
        for (int i = password.length(); i < length; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        // 打乱顺序
        return shuffleString(password.toString());
    }
    
    /**
     * 打乱字符串
     */
    private static String shuffleString(String input) {
        char[] chars = input.toCharArray();
        Random random = new SecureRandom();
        
        for (int i = chars.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = chars[i];
            chars[i] = chars[j];
            chars[j] = temp;
        }
        
        return new String(chars);
    }
    
    /**
     * 检查密码强度
     * 
     * @param password 密码
     * @return 强度等级（0-4，0最弱，4最强）
     */
    public static int checkPasswordStrength(String password) {
        if (password == null || password.isEmpty()) {
            return 0;
        }
        
        int strength = 0;
        
        // 长度检查
        if (password.length() >= 8) strength++;
        if (password.length() >= 12) strength++;
        
        // 字符类型检查
        if (password.matches(".*[A-Z].*")) strength++;
        if (password.matches(".*[a-z].*")) strength++;
        if (password.matches(".*[0-9].*")) strength++;
        if (password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{}|;:,.<>?].*")) strength++;
        
        // 最高强度为4
        return Math.min(strength, 4);
    }
    
    /**
     * 获取密码强度描述
     * 
     * @param strength 强度等级
     * @return 强度描述
     */
    public static String getPasswordStrengthDescription(int strength) {
        switch (strength) {
            case 0:
                return "非常弱";
            case 1:
                return "弱";
            case 2:
                return "中等";
            case 3:
                return "强";
            case 4:
                return "非常强";
            default:
                return "未知";
        }
    }
} 