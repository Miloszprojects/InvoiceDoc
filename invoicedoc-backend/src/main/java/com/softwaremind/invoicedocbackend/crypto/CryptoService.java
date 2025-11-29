package com.softwaremind.invoicedocbackend.crypto;

public interface CryptoService {
    String encrypt(String plaintext);
    String decrypt(String ciphertext);
}
