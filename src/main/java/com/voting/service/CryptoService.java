package com.voting.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@Service
public class CryptoService {

    @Value("${aes.secret.key}")
    private String secretKey;

    private SecretKeySpec keySpec() throws Exception {
        byte[] key = MessageDigest.getInstance("SHA-256")
                         .digest(secretKey.getBytes(StandardCharsets.UTF_8));
        byte[] k16 = new byte[16];
        System.arraycopy(key, 0, k16, 0, 16);
        return new SecretKeySpec(k16, "AES");
    }

    public String encrypt(String data) {
        try {
            Cipher c = Cipher.getInstance("AES/ECB/PKCS5Padding");
            c.init(Cipher.ENCRYPT_MODE, keySpec());
            return Base64.getEncoder().encodeToString(c.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) { throw new RuntimeException("Encryption failed", e); }
    }

    public String decrypt(String enc) {
        try {
            Cipher c = Cipher.getInstance("AES/ECB/PKCS5Padding");
            c.init(Cipher.DECRYPT_MODE, keySpec());
            return new String(c.doFinal(Base64.getDecoder().decode(enc)), StandardCharsets.UTF_8);
        } catch (Exception e) { throw new RuntimeException("Decryption failed", e); }
    }

    public String sha256(String input) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                              .digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { throw new RuntimeException("Hash failed", e); }
    }
}
