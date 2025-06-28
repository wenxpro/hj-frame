package com.wenx.v3secure.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.FileWriter;
import java.io.IOException;
import java.security.KeyPair;

/**
 * RSA密钥生成器
 * 用于生产环境生成RSA密钥文件的命令行工具
 * 
 * 使用方法：
 * 1. 运行 main 方法生成密钥对
 * 2. 将生成的 private_key.pem 和 public_key.pem 文件放到项目资源目录
 * 3. 在配置文件中指定密钥文件路径
 * 
 * @author wenx
 */
@Slf4j
public class RSAKeyGenerator {
    
    private static final int DEFAULT_KEY_SIZE = 2048;
    
    /**
     * 生成RSA密钥对并保存为PEM文件
     * 
     * @param keySize 密钥长度（推荐2048或4096）
     * @param outputDir 输出目录
     */
    public static void generateKeyPairFiles(int keySize, String outputDir) {
        try {
            // 生成密钥对
            KeyPair keyPair = RSAKeyUtils.generateKeyPair(keySize);
            
            // 转换为PEM格式
            String privateKeyPem = RSAKeyUtils.keyToPem(keyPair.getPrivate());
            String publicKeyPem = RSAKeyUtils.keyToPem(keyPair.getPublic());
            
            // 确保输出目录存在
            java.io.File dir = new java.io.File(outputDir);
            if (!dir.exists() && !dir.mkdirs()) {
                throw new IOException("无法创建输出目录: " + outputDir);
            }
            
            // 保存私钥文件
            String privateKeyPath = outputDir + "/private_key.pem";
            try (FileWriter writer = new FileWriter(privateKeyPath)) {
                writer.write(privateKeyPem);
            }
            
            // 保存公钥文件
            String publicKeyPath = outputDir + "/public_key.pem";
            try (FileWriter writer = new FileWriter(publicKeyPath)) {
                writer.write(publicKeyPem);
            }
            
            log.info("RSA密钥对生成成功:");
            log.info("私钥文件: {}", privateKeyPath);
            log.info("公钥文件: {}", publicKeyPath);
            log.info("密钥长度: {} bits", keySize);
            
            // 验证密钥对
            if (RSAKeyUtils.verifyKeyPair(keyPair.getPrivate(), keyPair.getPublic())) {
                log.info("密钥对验证成功");
            } else {
                log.error("密钥对验证失败");
            }
            
        } catch (Exception e) {
            log.error("生成RSA密钥对失败", e);
            throw new RuntimeException("生成RSA密钥对失败", e);
        }
    }
    
    /**
     * 打印密钥对信息（用于开发调试）
     * 
     * @param keySize 密钥长度
     */
    public static void generateAndPrintKeyPair(int keySize) {
        KeyPair keyPair = RSAKeyUtils.generateKeyPair(keySize);
        
        System.out.println("=== RSA私钥 (Private Key) ===");
        System.out.println(RSAKeyUtils.keyToPem(keyPair.getPrivate()));
        System.out.println();
        
        System.out.println("=== RSA公钥 (Public Key) ===");
        System.out.println(RSAKeyUtils.keyToPem(keyPair.getPublic()));
        System.out.println();
        
        // 验证密钥对
        boolean isValid = RSAKeyUtils.verifyKeyPair(keyPair.getPrivate(), keyPair.getPublic());
        System.out.println("密钥对验证结果: " + (isValid ? "有效" : "无效"));
        
        // 打印详细信息
        RSAKeyUtils.printKeyInfo(keyPair);
    }
    
    /**
     * 主方法 - 生成RSA密钥对文件
     * 
     * 使用方法：
     * java -cp your-classpath com.wenx.v3secure.utils.RSAKeyGenerator [keySize] [outputDir]
     * 
     * 参数：
     * keySize: 密钥长度，可选值：2048, 3072, 4096（默认2048）
     * outputDir: 输出目录（默认当前目录）
     */
    public static void main(String[] args) {
        int keySize = DEFAULT_KEY_SIZE;
        String outputDir = ".";
        boolean printOnly = false;
        
        // 解析命令行参数
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "--print":
                case "-p":
                    printOnly = true;
                    break;
                case "--help":
                case "-h":
                    printUsage();
                    return;
                default:
                    if (i == 0) {
                        try {
                            keySize = Integer.parseInt(arg);
                            if (keySize < 1024 || keySize > 8192) {
                                System.err.println("警告: 密钥长度建议在1024-8192之间，您指定的长度: " + keySize);
                            }
                        } catch (NumberFormatException e) {
                            System.err.println("无效的密钥长度: " + arg + "，使用默认值: " + DEFAULT_KEY_SIZE);
                            keySize = DEFAULT_KEY_SIZE;
                        }
                    } else if (i == 1) {
                        outputDir = arg;
                    }
                    break;
            }
        }
        
        System.out.println("=== V3安全模块RSA密钥生成器 ===");
        System.out.println("密钥长度: " + keySize + " bits");
        
        if (printOnly) {
            System.out.println("模式: 控制台打印");
            System.out.println();
            generateAndPrintKeyPair(keySize);
        } else {
            System.out.println("输出目录: " + outputDir);
            System.out.println();
            
            try {
                // 生成密钥对文件
                generateKeyPairFiles(keySize, outputDir);
                
                System.out.println();
                System.out.println("密钥生成完成！");
                System.out.println();
                System.out.println("下一步操作：");
                System.out.println("1. 将生成的密钥文件复制到项目资源目录");
                System.out.println("2. 在配置文件中指定密钥文件路径");
                System.out.println("3. 重启应用");
                
            } catch (Exception e) {
                System.err.println("密钥生成失败: " + e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }
        }
    }
    
    /**
     * 打印使用说明
     */
    private static void printUsage() {
        System.out.println("V3安全模块RSA密钥生成器");
        System.out.println();
        System.out.println("用法:");
        System.out.println("  java com.wenx.v3secure.utils.RSAKeyGenerator [选项] [密钥长度] [输出目录]");
        System.out.println();
        System.out.println("参数:");
        System.out.println("  密钥长度    RSA密钥的位长度 (默认: 2048)");
        System.out.println("  输出目录    密钥文件的输出目录 (默认: 当前目录)");
        System.out.println();
        System.out.println("选项:");
        System.out.println("  -p, --print    仅在控制台打印密钥，不保存文件");
        System.out.println("  -h, --help     显示此帮助信息");
        System.out.println();
        System.out.println("示例:");
        System.out.println("  # 生成2048位密钥到当前目录");
        System.out.println("  java com.wenx.v3secure.utils.RSAKeyGenerator");
        System.out.println();
        System.out.println("  # 生成4096位密钥到指定目录");
        System.out.println("  java com.wenx.v3secure.utils.RSAKeyGenerator 4096 ./keys");
        System.out.println();
        System.out.println("  # 仅在控制台打印密钥");
        System.out.println("  java com.wenx.v3secure.utils.RSAKeyGenerator --print 2048");
    }
} 