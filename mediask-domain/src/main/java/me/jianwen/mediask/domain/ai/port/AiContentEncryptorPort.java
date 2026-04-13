package me.jianwen.mediask.domain.ai.port;

public interface AiContentEncryptorPort {

    String encrypt(String plainText);

    String decrypt(String encryptedText);
}
