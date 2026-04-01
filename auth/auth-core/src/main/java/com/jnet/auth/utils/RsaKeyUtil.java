package com.jnet.auth.utils;

import com.nimbusds.jose.jwk.RSAKey;
import lombok.extern.slf4j.Slf4j;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * RSA 密钥工具类
 * 
 * <p>核心功能：管理 OAuth2 授权服务器的 RSA 密钥对</p>
 * 
 * <p>主要职责：</p>
 * <ul>
 *     <li>从文件加载已存在的 RSA 密钥对</li>
 *     <li>生成新的 RSA 密钥对（2048 位）</li>
 *     <li>将密钥对持久化到文件</li>
 *     <li>构建 JWK (JSON Web Key) 格式的密钥</li>
 * </ul>
 * 
 * <p>工作流程：</p>
 * <ol>
 *     <li>检查密钥文件（security/private-key.pem, security/public-key.pem）是否存在</li>
 *     <li>如果存在，从文件加载密钥对</li>
 *     <li>如果不存在，生成新的密钥对并保存到文件</li>
 *     <li>返回 JWK 格式的 RSA 密钥，用于 JWT Token 签名</li>
 * </ol>
 * 
 * <p>安全性说明：</p>
 * <ul>
 *     <li>使用 2048 位 RSA 密钥，符合当前安全标准</li>
 *     <li>密钥对持久化避免每次重启都生成新密钥</li>
 *     <li>私钥用于签名 JWT Token，公钥用于验证签名</li>
 * </ul>
 * 
 * @author mu
 * @version 1.0
 * @since 2026/4/1
 */
@Slf4j
public class RsaKeyUtil {
    /**
     * 加载或生成 RSA 密钥对
     *
     * <p>工作流程：</p>
     * <ol>
     *     <li>检查密钥文件是否存在</li>
     *     <li>如果存在，从文件加载密钥对</li>
     *     <li>如果不存在，生成新的密钥对并保存到文件</li>
     *     <li>构建 JWK 格式的密钥</li>
     * </ol>
     *
     * @return RSAKey JWK 格式的 RSA 密钥
     */
    public static RSAKey loadOrGenerateRsaKey() {
        try {
            // 尝试从文件加载密钥对
            File privateKeyFile = new File("security/private-key.pem");
            File publicKeyFile = new File("security/public-key.pem");

            if (privateKeyFile.exists() && publicKeyFile.exists()) {
                log.info("正在从文件加载已存在的 RSA 密钥对...");
                KeyPair keyPair = loadKeyPair(privateKeyFile, publicKeyFile);
                RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
                RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

                return new RSAKey.Builder(publicKey)
                        .privateKey(privateKey)
                        .keyID("rsa-key-1")
                        .build();
            } else {
                log.info("正在生成新的 RSA 密钥对...");
                // 生成新的密钥对
                KeyPair keyPair = generateKeyPair();
                RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
                RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

                // 保存密钥对到文件
                saveKeyPair(keyPair, privateKeyFile, publicKeyFile);

                return new RSAKey.Builder(publicKey)
                        .privateKey(privateKey)
                        .keyID("rsa-key-1")
                        .build();
            }
        } catch (Exception e) {
            throw new IllegalStateException("加载或生成 RSA 密钥对失败", e);
        }
    }

    /**
     * 生成 RSA 密钥对
     *
     * <p>使用 Java Cryptography Architecture (JCA) 生成 2048 位 RSA 密钥对</p>
     *
     * <p>安全性说明：</p>
     * <ul>
     *     <li>2048 位是当前推荐的最小密钥长度</li>
     *     <li>对于更高安全需求，可考虑 3072 或 4096 位</li>
     * </ul>
     *
     * @return KeyPair RSA 公私钥对
     * @throws IllegalStateException 密钥生成失败时抛出
     */
    private static KeyPair generateKeyPair() {
        try {
            // 获取 RSA 算法的 KeyPairGenerator 实例
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            // 初始化密钥长度为 2048 位
            keyPairGenerator.initialize(2048);
            // 生成密钥对
            return keyPairGenerator.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException("生成 RSA 密钥对失败", e);
        }
    }

    /**
     * 保存密钥对到文件
     *
     * <p>将生成的 RSA 密钥对保存到指定文件，使用 Base64 编码</p>
     * 
     * @param keyPair 密钥对
     * @param privateKeyFile 私钥文件路径
     * @param publicKeyFile 公钥文件路径
     * @throws IOException IO 异常
     */
    private static void saveKeyPair(KeyPair keyPair, File privateKeyFile, File publicKeyFile) throws IOException {
        // 确保目录存在
        privateKeyFile.getParentFile().mkdirs();

        // 保存私钥
        byte[] privateKeyBytes = keyPair.getPrivate().getEncoded();
        try (FileOutputStream fos = new FileOutputStream(privateKeyFile)) {
            fos.write(Base64.getEncoder().encode(privateKeyBytes));
        }

        // 保存公钥
        byte[] publicKeyBytes = keyPair.getPublic().getEncoded();
        try (FileOutputStream fos = new FileOutputStream(publicKeyFile)) {
            fos.write(Base64.getEncoder().encode(publicKeyBytes));
        }

        log.info("RSA 密钥对已保存到：{} 和 {}", privateKeyFile.getAbsolutePath(), publicKeyFile.getAbsolutePath());
    }

    /**
     * 从文件加载密钥对
     *
     * <p>从指定的 PEM 文件中加载 RSA 公钥和私钥</p>
     * 
     * @param privateKeyFile 私钥文件路径
     * @param publicKeyFile 公钥文件路径
     * @return 加载的密钥对
     * @throws IOException IO 异常
     * @throws NoSuchAlgorithmException 算法不存在
     * @throws InvalidKeySpecException 密钥规格无效
     */
    private static KeyPair loadKeyPair(File privateKeyFile, File publicKeyFile)
            throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        // 加载私钥
        byte[] privateKeyBytes = Base64.getDecoder().decode(Files.readAllBytes(privateKeyFile.toPath()));
        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);

        // 加载公钥
        byte[] publicKeyBytes = Base64.getDecoder().decode(Files.readAllBytes(publicKeyFile.toPath()));
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
        PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

        return new KeyPair(publicKey, privateKey);
    }
}
