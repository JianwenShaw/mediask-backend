package me.jianwen.mediask.infra.ai.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Base64;
import org.junit.jupiter.api.Test;

class AesGcmAiContentEncryptorTest {

    @Test
    void encrypt_ShouldReturnCipherText() {
        byte[] key = new byte[32];
        java.util.Arrays.fill(key, (byte) 7);
        AesGcmAiContentEncryptor encryptor = new AesGcmAiContentEncryptor(key);

        String cipherText = encryptor.encrypt("头痛三天");

        assertNotNull(cipherText);
        assertNotEquals("头痛三天", cipherText);
        Base64.getDecoder().decode(cipherText);
    }

    @Test
    void decrypt_ShouldRoundTripPlainText() {
        byte[] key = new byte[32];
        java.util.Arrays.fill(key, (byte) 7);
        AesGcmAiContentEncryptor encryptor = new AesGcmAiContentEncryptor(key);

        String cipherText = encryptor.encrypt("头痛三天");

        assertEquals("头痛三天", encryptor.decrypt(cipherText));
    }
}
