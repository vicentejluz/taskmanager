package com.vicente.taskmanager.security.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public final class CryptoHelper {
    // SecureRandom é um gerador de números aleatórios
    // criptograficamente seguro (CSPRNG).
    // Diferente de Random, ele NÃO é previsível.
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Logger logger = LoggerFactory.getLogger(CryptoHelper.class);

    private CryptoHelper() {}

    /**
     * Gera um valor aleatório criptograficamente seguro.
     * <p>
     * CONTEXTO:
     * Em sistemas de autenticação, diversos identificadores precisam ser
     * imprevisíveis para evitar ataques de adivinhação (guessing attacks).
     * <p>
     * Exemplos de uso incluem:
     * <p>
     * - Refresh tokens
     * - Session IDs
     * - Fingerprints de dispositivo
     * - Tokens de redefinição de senha
     * - Tokens de verificação de e-mail
     * <p>
     * Para esses casos, é essencial utilizar um gerador de números aleatórios
     * criptograficamente seguro (CSPRNG – Cryptographically Secure
     * Pseudo-Random Number Generator).
     * <p>
     * SOBRE SecureRandom:
     * A classe SecureRandom do Java utiliza fontes de entropia seguras do
     * sistema operacional para gerar números aleatórios imprevisíveis.
     * <p>
     * Diferente de java.util.Random:
     * <p>
     * Random:
     * - Determinístico
     * - Pode ser previsível
     * - NÃO deve ser usado para segurança
     * <p>
     * SecureRandom:
     * - Usa entropia do sistema operacional
     * - Não é previsível
     * - Apropriado para uso criptográfico
     * <p>
     * ENTROPIA GERADA:
     * Este método gera 32 bytes aleatórios (256 bits de entropia).
     * <p>
     * Isso significa:
     * <p>
     * 2^256 combinações possíveis
     * <p>
     * Esse nível de entropia é considerado extremamente seguro contra
     * ataques de força bruta, mesmo com grande poder computacional.
     * <p>
     * PROCESSO:
     * <p>
     * 1. Cria um array de 32 bytes
     * 2. Preenche o array com bytes aleatórios usando SecureRandom
     * 3. Converte os bytes para Base64 URL Safe
     * <p>
     * O uso de Base64 URL Safe garante que o valor:
     * <p>
     * - Seja seguro para uso em URLs
     * - Não contenha caracteres problemáticos (+, /)
     * - Não utilize padding "="
     * <p>
     * Isso torna o token adequado para uso em:
     * <p>
     * - Cookies HTTP
     * - Headers HTTP
     * - URLs
     * - Armazenamento textual
     *
     * @return cryptographically secure random value encoded in Base64 URL Safe
     */
    public static String generateSecureRandomValue() {
        byte[] bytes = new byte[32];

        SECURE_RANDOM.nextBytes(bytes);

        return encode(bytes);
    }

    /**
     * Gera o hash criptográfico SHA-256 de um valor fornecido.
     * <p>
     * CONTEXTO DE SEGURANÇA:
     * Em sistemas de autenticação seguros, valores sensíveis como:
     * <p>
     * - Refresh tokens
     * - Fingerprints de sessão
     * - API keys
     * - Tokens de verificação
     * <p>
     * não devem ser armazenados em texto puro no banco de dados.
     * <p>
     * Caso ocorra um vazamento do banco (data breach), um atacante poderia
     * utilizar esses valores diretamente para se autenticar ou assumir sessões.
     * <p>
     * Para evitar isso, o valor original é transformado em um hash criptográfico
     * antes de ser armazenado. Assim, mesmo que o banco seja comprometido,
     * o valor original não pode ser recuperado facilmente.
     * <p>
     * SOBRE SHA-256:
     * SHA-256 é uma função de hash criptográfica pertencente à família SHA-2.
     * <p>
     * Propriedades importantes:
     * <p>
     * - Determinística → a mesma entrada sempre gera o mesmo hash
     * - Irreversível → não é possível recuperar o valor original a partir do hash
     * - Tamanho fixo → sempre gera 256 bits (32 bytes)
     * - Resistente a colisões → extremamente difícil gerar dois valores diferentes
     *   que produzam o mesmo hash
     * <p>
     * PROCESSO DE HASH:
     * <p>
     * 1. Recebe um valor em formato String
     * 2. Converte a String para bytes usando UTF-8
     * 3. Aplica o algoritmo SHA-256
     * 4. O resultado é um array de 32 bytes (256 bits)
     * 5. Esse resultado é codificado em Base64 URL Safe para armazenamento
     * <p>
     * O uso de Base64 é necessário porque bytes não podem ser armazenados ou
     * transmitidos facilmente em formato textual.
     * <p>
     * USO NO SISTEMA:
     * Esse método pode ser utilizado para gerar o hash de:
     * <p>
     * - Refresh tokens antes de salvar no banco
     * - Fingerprints de dispositivo
     * - Tokens de verificação
     * - Identificadores de sessão
     *
     * @param value value original value to be hashed
     * @return SHA-256 hash encoded in Base64 URL Safe
     * @throws IllegalStateException if the SHA-256 algorithm is not available
     */
    public static String hashValue(String value) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

            byte[] hash = messageDigest.digest(value.getBytes(StandardCharsets.UTF_8));

            return encode(hash);
        } catch (NoSuchAlgorithmException e) {
            logger.error("Hash Algorithm Not Supported!", e);
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }


    /**
     * Compara duas Strings de forma segura utilizando comparação em tempo constante.
     * <p>
     * MOTIVAÇÃO:
     * Comparações tradicionais de String (ex: value1.equals(value2)) podem ser vulneráveis
     * a ataques conhecidos como "Timing Attacks".
     * <p>
     * Em uma comparação padrão, o algoritmo compara os caracteres sequencialmente e
     * interrompe assim que encontra uma diferença.
     * <p>
     * Exemplo de comparação tradicional:
     * <p>
     * value1 = "ABCDEFG"
     * value2 = "ABCXYZ"
     * <p>
     * Comparação:
     * A == A  ✔
     * B == B  ✔
     * C == C  ✔
     * D != X  ✘  → a comparação para aqui
     * <p>
     * Isso significa que quanto mais caracteres iniciais coincidirem,
     * mais tempo a comparação levará para retornar.
     * <p>
     * Um atacante pode explorar essa diferença de tempo enviando milhares de
     * requisições e medindo o tempo de resposta para inferir partes do valor correto.
     * Esse tipo de ataque é chamado de "Timing Attack" ou "Side Channel Attack".
     * <p>
     * Em sistemas que comparam dados sensíveis como:
     * <p>
     * - Hash de tokens
     * - Refresh token hashes
     * - API keys
     * - Assinaturas criptográficas
     * - HMACs
     * <p>
     * essa diferença de tempo pode permitir que um atacante descubra o valor correto
     * byte por byte.
     * <p>
     * SOLUÇÃO:
     * O método MessageDigest.isEqual() realiza uma comparação em tempo constante.
     * <p>
     * Isso significa que:
     * - Ele percorre todo o array de bytes sempre
     * - Não interrompe a comparação ao encontrar a primeira diferença
     * - O tempo de execução é praticamente o mesmo independentemente
     *   de quantos bytes são iguais ou diferentes
     * <p>
     * Dessa forma, não é possível inferir informações analisando o tempo de resposta.
     * <p>
     * IMPLEMENTAÇÃO:
     * Como MessageDigest.isEqual trabalha com byte[], os valores codificados em Base64
     * precisam primeiro ser decodificados para seus bytes originais antes da comparação.
     * <p>
     * PASSOS:
     * 1. Verifica se algum valor é null
     * 2. Decodifica as Strings Base64 para byte[]
     * 3. Compara os arrays usando MessageDigest.isEqual()
     * 4. Caso os valores não sejam Base64 válidos, retorna false
     * <p>
     * SEGURANÇA:
     * A comparação em tempo constante reduz significativamente o risco de
     * timing attacks ao validar valores sensíveis.
     * <p>
     * Embora essa técnica não elimine completamente todos os possíveis
     * canais laterais em ambientes extremamente controlados, ela é considerada
     * a prática recomendada para comparação de valores sensíveis em aplicações web.
     *
     * @param value1 first value to be compared
     * @param value2 second value to be compared
     * @return true if the values are identical, false otherwise
     */
    public static boolean safeEquals(String value1, String value2) {
        if (value1 == null || value2 == null) return false;

        try {
            var digestA = Base64.getUrlDecoder().decode(value1);
            byte[] digestB = Base64.getUrlDecoder().decode(value2);

            return MessageDigest.isEqual(digestA, digestB);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static String encode(byte[] bytes) {
        // Converte os bytes em String usando Base64 URL Safe.
        // Isso é necessário porque:
        // - Bytes não podem ser enviados diretamente em HTTP
        // - Base64 transforma bytes em texto
        // - UrlEncoder evita caracteres problemáticos (+, /)
        //
        // withoutPadding() remove o caractere '=' no final.
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
