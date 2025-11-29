package com.softwaremind.invoicedocbackend.crypto;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Service
@Slf4j
public class AesCryptoService implements CryptoService {

    private static final String ALGO = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;           // 96 bits – recommended for GCM
    private static final int TAG_LENGTH_BIT = 128;     // auth tag length

    @Value("${app.crypto.secret:change-me-secret}")
    private String secret;

    private SecretKeySpec secretKeySpec;
    private final SecureRandom secureRandom = new SecureRandom();

    @PostConstruct
    void init() {
        // Still a simple key; ideally derive with PBKDF2 from a password.
        byte[] keyBytes = Arrays.copyOf(secret.getBytes(StandardCharsets.UTF_8), 16); // 128-bit key
        this.secretKeySpec = new SecretKeySpec(keyBytes, ALGO);
    }

    @Override
    public String encrypt(String plain) {
        if (plain == null) return null;
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, gcmSpec);

            byte[] cipherBytes = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));

            // prepend IV to ciphertext
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherBytes.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherBytes);
            byte[] cipherWithIv = byteBuffer.array();

            return Base64.getEncoder().encodeToString(cipherWithIv);
        } catch (Exception e) {
            log.error("Cannot encrypt", e);
            throw new IllegalStateException("Cannot encrypt", e);
        }
    }

    @Override
    public String decrypt(String cipherText) {
        if (cipherText == null) return null;
        try {
            byte[] cipherWithIv = Base64.getDecoder().decode(cipherText);
            ByteBuffer byteBuffer = ByteBuffer.wrap(cipherWithIv);

            byte[] iv = new byte[IV_LENGTH];
            byteBuffer.get(iv);
            byte[] cipherBytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherBytes);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, gcmSpec);

            byte[] dec = cipher.doFinal(cipherBytes);
            return new String(dec, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Cannot decrypt", e);
            throw new IllegalStateException("Cannot decrypt", e);
        }
    }
}
