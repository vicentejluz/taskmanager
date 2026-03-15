package com.vicente.taskmanager.repository;

import com.vicente.taskmanager.domain.entity.RefreshToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.reuseDetected = true WHERE rt.id = :id AND rt.reuseDetected = false")
    void markReuseDetected(@Param("id") Long refreshTokenId);

    Optional<RefreshToken> findByToken(String token);

    List<RefreshToken> findByUser_IdAndRevokedAtNull(Long userId);
    List<RefreshToken> findByUser_IdAndTokenFamilyId(Long userId, UUID tokenFamilyId);
    List<RefreshToken> findByExpiresAtBefore(OffsetDateTime thresholdDate);
    List<RefreshToken> findByUser_DeletedAtBefore(OffsetDateTime thresholdDate);



    /**
     * Busca um refresh token pelo seu valor aplicando um lock pessimista (PESSIMISTIC_WRITE).

     * Por que esse lock é necessário:

     * Este método é utilizado durante o processo de refresh token rotation. Sem um lock
     * no banco de dados, duas requisições concorrentes poderiam ler o mesmo refresh token
     * antes que ele fosse revogado, causando uma race condition e permitindo que múltiplos
     * refresh tokens novos fossem gerados a partir de um único token antigo.

     * Exemplo de race condition sem lock:

     * Requisição A e Requisição B chegam quase ao mesmo tempo usando o mesmo refresh token.

     * Passo 1:
     * Ambas executam:
     *   SELECT * FROM refresh_tokens WHERE token = ?

     * Ambas encontram o token com:
     *   revoked = false

     * Passo 2:
     * A Requisição A revoga o token antigo e cria um novo refresh token (Token B).

     * Passo 3:
     * A Requisição B ainda acredita que o token é válido (pois leu antes da revogação)
     * e cria outro refresh token (Token C).

     * Resultado:
     *   Token B → válido
     *   Token C → válido

     * Isso quebra a garantia do refresh token rotation e pode permitir múltiplos
     * refresh tokens válidos derivados de um único token original.

     * O que o PESSIMISTIC_WRITE faz:

     * Ao aplicar PESSIMISTIC_WRITE, o banco executa algo equivalente a:

     *   SELECT * FROM refresh_tokens WHERE token = ? FOR UPDATE

     * Isso bloqueia (lock) a linha correspondente ao token até o final da transação atual.

     * Comportamento:
     * - A primeira requisição que ler o token adquire o lock.
     * - Outras requisições concorrentes que tentarem ler o mesmo token terão que esperar.
     * - Apenas quando a primeira transação finalizar (commit ou rollback) o lock será liberado.

     * Isso garante que apenas uma requisição consiga processar um refresh token
     * por vez, evitando race conditions durante a rotação de tokens.

     * Importante:
     * Este método precisa ser executado dentro de um método anotado com @Transactional,
     * caso contrário o lock não será aplicado corretamente pelo banco de dados.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.token = :token")
    Optional<RefreshToken> findByTokenForUpdate(@Param("token") String token);
}
