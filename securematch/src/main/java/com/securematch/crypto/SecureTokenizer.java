package com.securematch.crypto;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class SecureTokenizer {

    private final String secretKey;
    private static final int NGRAM_SIZE = 3;
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int TOKEN_LENGTH = 16;

    public SecureTokenizer(String secretKey) {
        if (secretKey == null || secretKey.isEmpty()) {
            throw new IllegalArgumentException("[X] Secret key cannot be null or empty");
        }
        this.secretKey = secretKey;
    }

    public String generateSalt() {
        return UUID.randomUUID()
                   .toString()
                   .replace("-", "")
                   .substring(0, 8);
    }

    public List<String> tokenize(String text, String salt) {
        if (text == null || text.trim().isEmpty()) {
            return new ArrayList<>();
        }

        String normalized = text.toLowerCase().trim();

        List<String> tokens = new ArrayList<>();

        for (int i = 0; i <= normalized.length() - NGRAM_SIZE; i++) {
            String chunk = normalized.substring(i, i + NGRAM_SIZE);
            String token = hmac(chunk + salt);
            tokens.add(token);
        }

        return tokens;
    }

    public double similarity(List<String> tokensA, List<String> tokensB) {
        if (tokensA == null || tokensB == null
                || tokensA.isEmpty() || tokensB.isEmpty()) {
            return 0.0;
        }

        Set<String> setA = new HashSet<>(tokensA);
        Set<String> setB = new HashSet<>(tokensB);

        Set<String> intersection = new HashSet<>(setA);
        intersection.retainAll(setB);

        Set<String> union = new HashSet<>(setA);
        union.addAll(setB);

        if (union.isEmpty()) return 0.0;

        return (double) intersection.size() / (double) union.size();
    }

    public String encryptAES(String plaintext) {
        try {
            byte[] iv = new byte[12];
            new SecureRandom().nextBytes(iv);

            byte[] keyBytes = deriveAESKey(secretKey);
            SecretKeySpec aesKey = new SecretKeySpec(keyBytes, "AES");

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(128, iv));

            byte[] ciphertext = cipher.doFinal(
                plaintext.getBytes(StandardCharsets.UTF_8)
            );

            return Base64.getEncoder().encodeToString(iv)
                + ":"
                + Base64.getEncoder().encodeToString(ciphertext);

        } catch (Exception e) {
            throw new RuntimeException("[X] AES encryption failed: " + e.getMessage(), e);
        }
    }

    public String decryptAES(String encryptedData) {
        try {
            String[] parts = encryptedData.split(":");
            if (parts.length != 2) {
                throw new RuntimeException("[X] Invalid encrypted data format");
            }

            byte[] iv         = Base64.getDecoder().decode(parts[0]);
            byte[] ciphertext = Base64.getDecoder().decode(parts[1]);

            byte[] keyBytes = deriveAESKey(secretKey);
            SecretKeySpec aesKey = new SecretKeySpec(keyBytes, "AES");

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, aesKey, new GCMParameterSpec(128, iv));

            byte[] decrypted = cipher.doFinal(ciphertext);
            return new String(decrypted, StandardCharsets.UTF_8);

        } catch (Exception e) {
            throw new RuntimeException(
                "[X] AES decryption failed. Wrong key or corrupted data: "
                + e.getMessage(), e
            );
        }
    }

    public List<String> generateTrapdoor(String query, String salt) {
        return tokenize(query, salt);
    }

    private byte[] deriveAESKey(String key) {
        byte[] keyBytes    = new byte[32];
        byte[] secretBytes = key.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(
            secretBytes, 0,
            keyBytes, 0,
            Math.min(secretBytes.length, 32)
        );
        return keyBytes;
    }

    private String hmac(String input) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);

            SecretKeySpec keySpec = new SecretKeySpec(
                secretKey.getBytes(StandardCharsets.UTF_8),
                HMAC_ALGORITHM
            );

            mac.init(keySpec);

            byte[] rawBytes = mac.doFinal(
                input.getBytes(StandardCharsets.UTF_8)
            );

            return bytesToHex(rawBytes).substring(0, TOKEN_LENGTH);

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("[X] HmacSHA256 not available in this JVM", e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException("[X] Invalid HMAC key — check SECRET_KEY in .env", e);
        } catch (Exception e) {
            throw new RuntimeException("[X] HMAC failed: " + e.getMessage(), e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
