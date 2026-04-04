package com.vicente.taskmanager.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SslOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.io.File;
import java.time.Duration;

@Configuration
public class RedisConfig {
    @Value("${spring.data.redis.host}")
    private String host;
    @Value("${spring.data.redis.port}")
    private int port;
    @Value("${spring.data.redis.password}")
    private String password;

    @Value("${redis.ca}")
    private File ca;

    @Bean
    @Profile("dev")
    public RedisConnectionFactory redisLocalConnectionFactory() {
        // Configuração básica do Redis standalone (host, porta e senha)
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(host, port);
        config.setPassword(password); // Define a senha do Redis, obrigatória se o Redis exigir autenticação

        // Configuração do cliente Lettuce (cliente padrão do Spring Boot para Redis)
        LettuceClientConfiguration clientConf = LettuceClientConfiguration
                .builder()
                // Habilita SSL/TLS, necessário para conexões seguras com Redis (como no Azure Redis)
                .useSsl()
                .and()
                // Define o timeout máximo para comandos Redis (evita que requisições travem indefinidamente)
                .commandTimeout(Duration.ofSeconds(10))
                // Configurações adicionais do cliente Lettuce
                .clientOptions(ClientOptions
                        .builder()
                        // Configura SSL com o certificado da CA, garantindo que o Java confie no certificado do servidor Redis
                        .sslOptions(SslOptions.builder()
                                .jdkSslProvider() // Usa o provedor SSL nativo do Java (JSSE)
                                .trustManager(ca) // Arquivo .crt da CA que assinou o certificado do servidor
                                .build())
                        // Habilita reconexão automática caso a conexão seja perdida
                        // Essencial para ambientes de cloud, onde a rede pode ter instabilidade
                        .autoReconnect(true)
                        .build())
                .build();

        // Cria a ConnectionFactory usando:
        // 1. Configuração standalone do Redis (host, porta, senha)
        // 2. Configuração do cliente Lettuce (SSL, timeout, reconexão)
        // Lettuce gerencia internamente pool de conexões e threads, permitindo uso eficiente do Redis
        return new LettuceConnectionFactory(config, clientConf);
    }

    @Bean
    @Profile("prod")
    public RedisConnectionFactory redisAzureConnectionFactory() {
        // Configuração básica para conexão com Redis standalone (não cluster)
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();

        // Define o hostname do Redis (ex: redis-study.redis.cache.windows.net)
        config.setHostName(host);

        // Define a porta (Azure Redis usa 6380 com SSL)
        config.setPort(port);

        // Define a senha de autenticação (Primary ou Secondary key do Azure)
        config.setPassword(password);

        // Configuração do cliente Lettuce (cliente padrão do Spring Boot para Redis)
        LettuceClientConfiguration clientConf = LettuceClientConfiguration
                .builder()
                // Habilita SSL (obrigatório no Azure Cache for Redis)
                .useSsl()
                .and()
                // Tempo máximo para execução de um comando Redis
                // Evita que requisições fiquem travadas indefinidamente
                .commandTimeout(Duration.ofSeconds(10))
                // Opções adicionais do cliente Lettuce
                .clientOptions(ClientOptions
                        .builder()
                        // Reconecta automaticamente caso a conexão caia
                        // Importante para ambientes cloud (Azure/AWS)
                        .autoReconnect(true)
                        .build())
                .build();

        // Cria a ConnectionFactory usando:
        // - Configuração standalone (host, porta, senha)
        // - Configuração do cliente (SSL, timeout, reconnect)
        // Lettuce é o cliente padrão e gerencia pool e conexões internamente
        return new LettuceConnectionFactory(config, clientConf);
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        // Cria um template que usa String para chave e valor
        // Ideal para casos simples como cache ou blacklist de JWT
        return new StringRedisTemplate(connectionFactory);
    }

    /*
     * RedisTemplate configurado para salvar objetos Java
     * usando JSON como formato de armazenamento.
     * Usado quando queremos armazenar entidades completas.
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {

        // Serializer que converte objetos Java em JSON usando Jackson
        GenericJacksonJsonRedisSerializer serializer =
                GenericJacksonJsonRedisSerializer.builder().build();

        // Template principal para operações Redis
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();

        // Conexão com Redis
        redisTemplate.setConnectionFactory(connectionFactory);

        // Chaves salvas como String
        redisTemplate.setKeySerializer(new StringRedisSerializer());

        // Valores convertidos para JSON
        redisTemplate.setValueSerializer(serializer);

        // Chaves dentro de HASH como String
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());

        // Valores dentro de HASH como JSON
        redisTemplate.setHashValueSerializer(serializer);

        // Inicializa propriedades do template
        redisTemplate.afterPropertiesSet();

        return redisTemplate;
    }
}

/*
 * RedisTemplate configurado manualmente para String
 * Faz basicamente a mesma coisa que StringRedisTemplate,
 * mas com configuração explícita de serializers.
 */
/*    @Bean
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
 */
