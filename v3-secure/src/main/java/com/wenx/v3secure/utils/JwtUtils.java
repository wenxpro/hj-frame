package com.wenx.v3secure.utils;

import io.jsonwebtoken.*;
import lombok.extern.slf4j.Slf4j;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * JWT工具类
 * 提供JWT Token的生成、解析、验证等通用功能
 * 
 * @author wenx
 */
@Slf4j
public class JwtUtils {
    
    /**
     * 生成JWT Token（使用RS256算法）
     * 
     * @param privateKey 私钥
     * @param issuer 发行者
     * @param subject 主题
     * @param audience 受众
     * @param expiration 过期时间
     * @param claims 自定义声明
     * @return JWT Token
     */
    public static String generateToken(PrivateKey privateKey, String issuer, String subject, 
                                     String audience, Date expiration, Map<String, Object> claims) {
        Date now = new Date();
        
        JwtBuilder builder = Jwts.builder()
                .header()
                    .type("JWT")
                    .and()
                .issuer(issuer)
                .subject(subject)
                .audience().add(audience).and()
                .issuedAt(now)
                .expiration(expiration)
                .id(UUID.randomUUID().toString())
                .signWith(privateKey, SignatureAlgorithm.RS256);
        
        // 添加自定义声明
        if (claims != null && !claims.isEmpty()) {
            claims.forEach(builder::claim);
        }
        
        // 添加算法标识
        builder.claim("algorithm", "RS256");
        
        return builder.compact();
    }
    
    /**
     * 生成JWT Token（简化版本）
     * 
     * @param privateKey 私钥
     * @param issuer 发行者
     * @param subject 主题
     * @param expirationSeconds 过期时间（秒）
     * @param claims 自定义声明
     * @return JWT Token
     */
    public static String generateToken(PrivateKey privateKey, String issuer, String subject, 
                                     long expirationSeconds, Map<String, Object> claims) {
        Date expiration = new Date(System.currentTimeMillis() + expirationSeconds * 1000);
        return generateToken(privateKey, issuer, subject, issuer, expiration, claims);
    }
    
    /**
     * 解析JWT Token（使用RS256算法验证）
     * 
     * @param token JWT Token
     * @param publicKey 公钥
     * @param issuer 期望的发行者
     * @param audience 期望的受众
     * @return Claims对象
     */
    public static Claims parseToken(String token, PublicKey publicKey, String issuer, String audience) {
        try {
            JwtParserBuilder parserBuilder = Jwts.parser()
                    .verifyWith(publicKey);
            
            if (issuer != null) {
                parserBuilder.requireIssuer(issuer);
            }
            
            if (audience != null) {
                parserBuilder.requireAudience(audience);
            }
            
            return parserBuilder.build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.warn("Token已过期: {}", e.getMessage());
            throw new RuntimeException("Token已过期");
        } catch (UnsupportedJwtException e) {
            log.warn("不支持的Token格式: {}", e.getMessage());
            throw new RuntimeException("不支持的Token格式");
        } catch (MalformedJwtException e) {
            log.warn("Token格式错误: {}", e.getMessage());
            throw new RuntimeException("Token格式错误");
        } catch (io.jsonwebtoken.security.SignatureException e) {
            log.warn("Token签名验证失败: {}", e.getMessage());
            throw new RuntimeException("Token签名验证失败");
        } catch (IllegalArgumentException e) {
            log.warn("Token参数错误: {}", e.getMessage());
            throw new RuntimeException("Token参数错误");
        } catch (Exception e) {
            log.error("Token解析异常", e);
            throw new RuntimeException("Token解析失败");
        }
    }
    
    /**
     * 解析JWT Token（简化版本，不验证发行者和受众）
     * 
     * @param token JWT Token
     * @param publicKey 公钥
     * @return Claims对象
     */
    public static Claims parseToken(String token, PublicKey publicKey) {
        return parseToken(token, publicKey, null, null);
    }
    
    /**
     * 验证JWT Token是否有效
     * 
     * @param token JWT Token
     * @param publicKey 公钥
     * @param issuer 期望的发行者
     * @param audience 期望的受众
     * @return 是否有效
     */
    public static boolean validateToken(String token, PublicKey publicKey, String issuer, String audience) {
        try {
            parseToken(token, publicKey, issuer, audience);
            return true;
        } catch (Exception e) {
            log.debug("Token验证失败: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 验证JWT Token是否有效（简化版本）
     * 
     * @param token JWT Token
     * @param publicKey 公钥
     * @return 是否有效
     */
    public static boolean validateToken(String token, PublicKey publicKey) {
        return validateToken(token, publicKey, null, null);
    }
    
    /**
     * 从Token中获取指定的声明值
     * 
     * @param token JWT Token
     * @param publicKey 公钥
     * @param claimName 声明名称
     * @param claimType 声明类型
     * @return 声明值
     */
    public static <T> T getClaimFromToken(String token, PublicKey publicKey, String claimName, Class<T> claimType) {
        Claims claims = parseToken(token, publicKey);
        return claims.get(claimName, claimType);
    }
    
    /**
     * 从Token中获取主题
     * 
     * @param token JWT Token
     * @param publicKey 公钥
     * @return 主题
     */
    public static String getSubjectFromToken(String token, PublicKey publicKey) {
        Claims claims = parseToken(token, publicKey);
        return claims.getSubject();
    }
    
    /**
     * 从Token中获取过期时间
     * 
     * @param token JWT Token
     * @param publicKey 公钥
     * @return 过期时间
     */
    public static Date getExpirationFromToken(String token, PublicKey publicKey) {
        Claims claims = parseToken(token, publicKey);
        return claims.getExpiration();
    }
    
    /**
     * 检查Token是否过期
     * 
     * @param token JWT Token
     * @param publicKey 公钥
     * @return 是否过期
     */
    public static boolean isTokenExpired(String token, PublicKey publicKey) {
        try {
            Date expiration = getExpirationFromToken(token, publicKey);
            return expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
    
    /**
     * 获取Token的剩余有效时间（秒）
     * 
     * @param token JWT Token
     * @param publicKey 公钥
     * @return 剩余有效时间（秒），如果已过期返回0
     */
    public static long getTokenRemainingTime(String token, PublicKey publicKey) {
        try {
            Date expiration = getExpirationFromToken(token, publicKey);
            long remaining = (expiration.getTime() - System.currentTimeMillis()) / 1000;
            return Math.max(0, remaining);
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * 从Authorization Header中提取Token
     * 
     * @param authorizationHeader Authorization头部值
     * @return JWT Token，如果格式不正确返回null
     */
    public static String extractTokenFromHeader(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        return null;
    }
    
    /**
     * 创建Authorization Header值
     * 
     * @param token JWT Token
     * @return Authorization头部值
     */
    public static String createAuthorizationHeader(String token) {
        return "Bearer " + token;
    }
} 