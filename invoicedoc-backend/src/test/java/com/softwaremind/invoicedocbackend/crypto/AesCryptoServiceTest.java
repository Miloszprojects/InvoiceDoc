package com.softwaremind.invoicedocbackend.crypto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AesCryptoServiceTest {

    private static final String SECRET_1 = "SuperTajnySekretDoTestow123";
    private static final String SECRET_2 = "InnySekretDoTestowJwtService";

    private AesCryptoService createServiceWithSecret(String secret) throws Exception {
        AesCryptoService service = new AesCryptoService();

        Field secretField = AesCryptoService.class.getDeclaredField("secret");
        secretField.setAccessible(true);
        secretField.set(service, secret);

        service.init();

        return service;
    }

    @Test
    @DisplayName("encrypt and decrypt should be symmetric for normal ASCII text")
    void encryptDecryptShouldBeSymmetricForAscii() throws Exception {
        AesCryptoService service = createServiceWithSecret(SECRET_1);
        String plain = "Hello AES-GCM!";

        String cipher = service.encrypt(plain);
        String decrypted = service.decrypt(cipher);

        assertAll(
                () -> assertThat(cipher).isNotNull().isNotEmpty(),
                () -> assertThat(decrypted).isEqualTo(plain)
        );
    }

    @Test
    @DisplayName("encrypt and decrypt should be symmetric for UTF-8 text with Polish characters")
    void encryptDecryptShouldBeSymmetricForUtf8() throws Exception {
        AesCryptoService service = createServiceWithSecret(SECRET_1);
        String plain = "Zażółć gęślą jaźń ąęśćłó";

        String cipher = service.encrypt(plain);
        String decrypted = service.decrypt(cipher);

        assertAll(
                () -> assertThat(cipher).isNotNull().isNotEmpty(),
                () -> assertThat(decrypted).isEqualTo(plain)
        );
    }

    @Test
    @DisplayName("encrypt and decrypt should be symmetric for empty string")
    void encryptDecryptShouldBeSymmetricForEmptyString() throws Exception {
        AesCryptoService service = createServiceWithSecret(SECRET_1);
        String plain = "";

        String cipher = service.encrypt(plain);
        String decrypted = service.decrypt(cipher);

        assertAll(
                () -> assertThat(cipher).isNotNull().isNotEmpty(),
                () -> assertThat(decrypted).isEqualTo(plain)
        );
    }

    @Test
    @DisplayName("encrypt should return different ciphertexts for the same plaintext due to random IV")
    void encryptShouldReturnDifferentCiphertextsForSamePlaintext() throws Exception {
        AesCryptoService service = createServiceWithSecret(SECRET_1);
        String plain = "Same text";

        String cipher1 = service.encrypt(plain);
        String cipher2 = service.encrypt(plain);

        assertAll(
                () -> assertThat(cipher1).isNotNull().isNotEmpty(),
                () -> assertThat(cipher2).isNotNull().isNotEmpty(),
                () -> assertThat(cipher1).isNotEqualTo(cipher2),
                () -> assertThat(service.decrypt(cipher1)).isEqualTo(plain),
                () -> assertThat(service.decrypt(cipher2)).isEqualTo(plain)
        );
    }

    @Test
    @DisplayName("encrypt should return null when input is null")
    void encryptShouldReturnNullWhenInputIsNull() throws Exception {
        AesCryptoService service = createServiceWithSecret(SECRET_1);

        String result = service.encrypt(null);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("decrypt should return null when input is null")
    void decryptShouldReturnNullWhenInputIsNull() throws Exception {
        AesCryptoService service = createServiceWithSecret(SECRET_1);

        String result = service.decrypt(null);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("decrypt should throw IllegalStateException for tampered ciphertext")
    void decryptShouldThrowIllegalStateExceptionForTamperedCiphertext() throws Exception {
        AesCryptoService service = createServiceWithSecret(SECRET_1);
        String plain = "Sensitive data";

        String cipher = service.encrypt(plain);
        String tampered = cipher.substring(0, cipher.length() - 2) + "==";

        assertThrows(IllegalStateException.class, () -> service.decrypt(tampered));
    }

    @Test
    @DisplayName("decrypt should throw IllegalStateException when ciphertext created with different secret")
    void decryptShouldThrowIllegalStateExceptionWhenSecretIsDifferent() throws Exception {
        AesCryptoService service1 = createServiceWithSecret(SECRET_1);
        AesCryptoService service2 = createServiceWithSecret(SECRET_2);

        String plain = "Secret message";

        String cipherFromService1 = service1.encrypt(plain);

        assertThrows(IllegalStateException.class, () -> service2.decrypt(cipherFromService1));
    }
}
