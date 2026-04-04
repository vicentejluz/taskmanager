package com.vicente.storage.security;

import com.vicente.storage.util.StorageLogger;
import org.slf4j.event.Level;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public final class LocalStorageHmacTokenGenerator {
    private LocalStorageHmacTokenGenerator() {}

    // Método público estático que gera um HMAC usando SHA-256 e retorna a string codificada em Base64URL
    public static String generateHmac256(String secret, String data) {
        // Define o algoritmo de HMAC a ser usado
        String algorithm = "HmacSHA256";

        // Decodifica a chave secreta em Base64 para bytes.
        // O segredo vem como String, mas o HMAC precisa de uma chave binária.
        byte[] keyBytes = Base64.getDecoder().decode(secret.getBytes(StandardCharsets.UTF_8));

        // Cria uma chave secreta do tipo HMAC usando os bytes decodificados e o algoritmo especificado
        SecretKey secretKey = new SecretKeySpec(keyBytes, algorithm);

        byte[] bytes; // Array que vai armazenar o resultado do HMAC
        try {
            // Chama o método auxiliar que faz o cálculo do HMAC real
            bytes = hmac(algorithm, secretKey, data.getBytes());
        } catch (NoSuchAlgorithmException e) {
            // Caso o algoritmo HmacSHA256 não esteja disponível na JVM, loga o erro e lança uma exceção personalizada
            throw StorageLogger.logAndCreateException(
                    Level.ERROR,
                    "HMAC algorithm not available while generating token | data={}",
                    "Error generating HMAC token",
                    500,
                    e,
                    data
            );
        } catch (InvalidKeyException e) {
            // Caso a chave fornecida seja inválida para HMAC, loga e lança exceção personalizada
            throw StorageLogger.logAndCreateException(
                    Level.ERROR,
                    "Invalid HMAC secret key while generating token | data={}",
                    "Error generating HMAC token",
                    500,
                    e,
                    data
            );
        }

        // Codifica o resultado do HMAC em Base64URL sem padding, para gerar uma string segura para URLs
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    // Método auxiliar privado que calcula o HMAC de um array de bytes usando a chave e algoritmo especificados
    private static byte[] hmac(String algorithm, Key key, byte[] data) throws NoSuchAlgorithmException, InvalidKeyException {
        // Cria um objeto Mac para o algoritmo especificado (HmacSHA256)
        Mac mac = Mac.getInstance(algorithm);

        // Inicializa o Mac com a chave secreta
        mac.init(key);

        // Calcula o HMAC dos dados e retorna como array de bytes
        return mac.doFinal(data);
    }

}
