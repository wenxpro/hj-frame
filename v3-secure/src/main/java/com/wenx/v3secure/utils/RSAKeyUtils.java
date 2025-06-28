package com.wenx.v3secure.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * RSA密钥工具类
 * 提供RSA密钥对的生成、加载、验证等通用功能
 * 
 * @author wenx
 */
@Slf4j
public class RSAKeyUtils {
    
    private static final String KEY_ALGORITHM = "RSA";
    private static final int DEFAULT_KEY_SIZE = 2048;
    
    /**
     * 生成RSA密钥对
     * 
     * @param keySize 密钥长度（推荐2048或4096）
     * @return 密钥对
     */
    public static KeyPair generateKeyPair(int keySize) {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(KEY_ALGORITHM);
            keyPairGenerator.initialize(keySize);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            
            log.info("RSA密钥对生成成功，密钥长度: {} bits", keySize);
            return keyPair;
        } catch (NoSuchAlgorithmException e) {
            log.error("生成RSA密钥对失败", e);
            throw new RuntimeException("生成RSA密钥对失败", e);
        }
    }
    
    /**
     * 生成默认长度的RSA密钥对
     * 
     * @return 密钥对
     */
    public static KeyPair generateKeyPair() {
        return generateKeyPair(DEFAULT_KEY_SIZE);
    }
    
    /**
     * 从PEM格式字符串加载私钥
     * 
     * @param privateKeyPem PEM格式的私钥字符串
     * @return 私钥对象
     */
    public static PrivateKey loadPrivateKeyFromPem(String privateKeyPem) {
        try {
            // 移除PEM头尾和换行符
            String privateKeyContent = privateKeyPem
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                    .replace("-----END RSA PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            
            // Base64解码
            byte[] keyBytes = Base64.getDecoder().decode(privateKeyContent);
            
            // 创建私钥
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
            
            log.debug("私钥加载成功");
            return keyFactory.generatePrivate(spec);
        } catch (Exception e) {
            log.error("加载私钥失败", e);
            throw new RuntimeException("加载私钥失败", e);
        }
    }
    
    /**
     * 从PEM格式字符串加载公钥
     * 
     * @param publicKeyPem PEM格式的公钥字符串
     * @return 公钥对象
     */
    public static PublicKey loadPublicKeyFromPem(String publicKeyPem) {
        try {
            // 移除PEM头尾和换行符
            String publicKeyContent = publicKeyPem
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replace("-----BEGIN RSA PUBLIC KEY-----", "")
                    .replace("-----END RSA PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");
            
            // Base64解码
            byte[] keyBytes = Base64.getDecoder().decode(publicKeyContent);
            
            // 创建公钥
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
            
            log.debug("公钥加载成功");
            return keyFactory.generatePublic(spec);
        } catch (Exception e) {
            log.error("加载公钥失败", e);
            throw new RuntimeException("加载公钥失败", e);
        }
    }
    
    /**
     * 从classpath资源文件加载私钥
     * 
     * @param resourcePath 资源文件路径
     * @return 私钥对象
     */
    public static PrivateKey loadPrivateKeyFromResource(String resourcePath) {
        try {
            Resource resource = new ClassPathResource(resourcePath);
            byte[] keyBytes = FileCopyUtils.copyToByteArray(resource.getInputStream());
            String keyContent = new String(keyBytes, StandardCharsets.UTF_8);
            
            log.info("从资源文件加载私钥: {}", resourcePath);
            return loadPrivateKeyFromPem(keyContent);
        } catch (IOException e) {
            log.error("从资源文件加载私钥失败: {}", resourcePath, e);
            throw new RuntimeException("从资源文件加载私钥失败: " + resourcePath, e);
        }
    }
    
    /**
     * 从classpath资源文件加载公钥
     * 
     * @param resourcePath 资源文件路径
     * @return 公钥对象
     */
    public static PublicKey loadPublicKeyFromResource(String resourcePath) {
        try {
            Resource resource = new ClassPathResource(resourcePath);
            byte[] keyBytes = FileCopyUtils.copyToByteArray(resource.getInputStream());
            String keyContent = new String(keyBytes, StandardCharsets.UTF_8);
            
            log.info("从资源文件加载公钥: {}", resourcePath);
            return loadPublicKeyFromPem(keyContent);
        } catch (IOException e) {
            log.error("从资源文件加载公钥失败: {}", resourcePath, e);
            throw new RuntimeException("从资源文件加载公钥失败: " + resourcePath, e);
        }
    }
    
    /**
     * 将密钥转换为PEM格式字符串
     * 
     * @param key 密钥对象
     * @return PEM格式字符串
     */
    public static String keyToPem(Key key) {
        String base64Key = Base64.getEncoder().encodeToString(key.getEncoded());
        
        // 格式化为PEM格式（每64个字符换行）
        StringBuilder pemBuilder = new StringBuilder();
        
        if (key instanceof PrivateKey) {
            pemBuilder.append("-----BEGIN PRIVATE KEY-----\n");
        } else if (key instanceof PublicKey) {
            pemBuilder.append("-----BEGIN PUBLIC KEY-----\n");
        }
        
        // 每64个字符换行
        for (int i = 0; i < base64Key.length(); i += 64) {
            int endIndex = Math.min(i + 64, base64Key.length());
            pemBuilder.append(base64Key, i, endIndex).append("\n");
        }
        
        if (key instanceof PrivateKey) {
            pemBuilder.append("-----END PRIVATE KEY-----");
        } else if (key instanceof PublicKey) {
            pemBuilder.append("-----END PUBLIC KEY-----");
        }
        
        return pemBuilder.toString();
    }
    
    /**
     * 验证密钥对是否匹配
     * 
     * @param privateKey 私钥
     * @param publicKey 公钥
     * @return 是否匹配
     */
    public static boolean verifyKeyPair(PrivateKey privateKey, PublicKey publicKey) {
        try {
            // 使用私钥签名一个测试数据
            String testData = "test-key-pair-verification-" + System.currentTimeMillis();
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(testData.getBytes(StandardCharsets.UTF_8));
            byte[] signatureBytes = signature.sign();
            
            // 使用公钥验证签名
            signature.initVerify(publicKey);
            signature.update(testData.getBytes(StandardCharsets.UTF_8));
            boolean verified = signature.verify(signatureBytes);
            
            log.debug("密钥对验证结果: {}", verified);
            return verified;
        } catch (Exception e) {
            log.error("验证密钥对失败", e);
            return false;
        }
    }
    
    /**
     * 获取RSA密钥的位长度
     * 
     * @param key RSA密钥（公钥或私钥）
     * @return 密钥位长度
     */
    public static int getKeySize(Key key) {
        if (key instanceof java.security.interfaces.RSAKey) {
            java.security.interfaces.RSAKey rsaKey = (java.security.interfaces.RSAKey) key;
            return rsaKey.getModulus().bitLength();
        }
        throw new IllegalArgumentException("不是有效的RSA密钥");
    }
    
    /**
     * 获取公钥的Base64编码
     * 
     * @param publicKey 公钥
     * @return Base64编码字符串
     */
    public static String getPublicKeyBase64(PublicKey publicKey) {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }
    
    /**
     * 从Base64编码加载公钥
     * 
     * @param base64Key Base64编码的公钥
     * @return 公钥对象
     */
    public static PublicKey loadPublicKeyFromBase64(String base64Key) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(base64Key);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
            return keyFactory.generatePublic(spec);
        } catch (Exception e) {
            log.error("从Base64加载公钥失败", e);
            throw new RuntimeException("从Base64加载公钥失败", e);
        }
    }
    
    /**
     * 获取私钥的Base64编码
     * 
     * @param privateKey 私钥
     * @return Base64编码字符串
     */
    public static String getPrivateKeyBase64(PrivateKey privateKey) {
        return Base64.getEncoder().encodeToString(privateKey.getEncoded());
    }
    
    /**
     * 从Base64编码加载私钥
     * 
     * @param base64Key Base64编码的私钥
     * @return 私钥对象
     */
    public static PrivateKey loadPrivateKeyFromBase64(String base64Key) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(base64Key);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
            return keyFactory.generatePrivate(spec);
        } catch (Exception e) {
            log.error("从Base64加载私钥失败", e);
            throw new RuntimeException("从Base64加载私钥失败", e);
        }
    }
    
    /**
     * 打印密钥信息（用于调试）
     * 
     * @param keyPair 密钥对
     */
    public static void printKeyInfo(KeyPair keyPair) {
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();
        
        System.out.println("=== RSA密钥对信息 ===");
        System.out.println("私钥算法: " + privateKey.getAlgorithm());
        System.out.println("私钥格式: " + privateKey.getFormat());
        System.out.println("公钥算法: " + publicKey.getAlgorithm());
        System.out.println("公钥格式: " + publicKey.getFormat());
        System.out.println("密钥长度: " + getKeySize(privateKey) + " bits");
        System.out.println("密钥对验证: " + (verifyKeyPair(privateKey, publicKey) ? "✅ 成功" : "❌ 失败"));
    }
} 