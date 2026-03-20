package com.vicente.taskmanager.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class RedisConfig {
    @Value("${spring.data.redis.host}")
    private String host;
    @Value("${spring.data.redis.port}")
    private int port;
    @Value("${spring.data.redis.password}")
    private String password;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(host);
        config.setPort(port);
        config.setPassword(password);

        // Implementação baseada no cliente Lettuce (padrão do Spring Boot)
        // Ele gerencia pool de conexões e comunicação com Redis
        return new LettuceConnectionFactory(config);
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        // Cria um template que usa String para chave e valor
        // Ideal para casos simples como cache ou blacklist de JWT
        return new StringRedisTemplate(connectionFactory);
    }

    /*
     * RedisTemplate configurado manualmente para String
     * Faz basicamente a mesma coisa que StringRedisTemplate,
     * mas com configuração explícita de serializers.
     */
//    @Bean
//    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
//
//        // Cria o template principal para operações Redis
//        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
//
//        // Define qual conexão Redis será usada
//        redisTemplate.setConnectionFactory(connectionFactory);
//
//        // Define que as chaves serão serializadas como String
//        redisTemplate.setKeySerializer(new StringRedisSerializer());
//
//        // Define que os valores também serão Strings
//        redisTemplate.setValueSerializer(new StringRedisSerializer());
//
//        // Serializer usado para chaves dentro de estruturas HASH
//        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
//
//        // Serializer usado para valores dentro de HASH
//        redisTemplate.setHashValueSerializer(new StringRedisSerializer());
//
//        // Inicializa e valida as propriedades configuradas
//        redisTemplate.afterPropertiesSet();
//
//        return redisTemplate;
//    }

    /*
     * RedisTemplate configurado para salvar objetos Java
     * usando JSON como formato de armazenamento.
     * Usado quando queremos armazenar entidades completas.
     */
//    @Bean
//    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
//
//        // Serializer que converte objetos Java em JSON usando Jackson
//        GenericJacksonJsonRedisSerializer serializer =
//                GenericJacksonJsonRedisSerializer.builder().build();
//
//        // Template principal para operações Redis
//        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
//
//        // Conexão com Redis
//        redisTemplate.setConnectionFactory(connectionFactory);
//
//        // Chaves salvas como String
//        redisTemplate.setKeySerializer(new StringRedisSerializer());
//
//        // Valores convertidos para JSON
//        redisTemplate.setValueSerializer(serializer);
//
//        // Chaves dentro de HASH como String
//        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
//
//        // Valores dentro de HASH como JSON
//        redisTemplate.setHashValueSerializer(serializer);
//
//        // Inicializa propriedades do template
//        redisTemplate.afterPropertiesSet();
//
//        return redisTemplate;
//    }
}

