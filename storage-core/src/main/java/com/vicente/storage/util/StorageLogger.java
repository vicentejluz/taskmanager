package com.vicente.storage.util;

import com.vicente.storage.exception.StorageException;
import org.jetbrains.annotations.Contract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

public final class StorageLogger {
    private static final Logger logger = LoggerFactory.getLogger(StorageLogger.class);

    private StorageLogger() {}

    /**
     * Lança uma exceção de armazenamento após registrar uma mensagem de log no nível especificado.
     * <p>
     * Este método registra a mensagem usando o nível fornecido e então lança uma {@link StorageException}
     * com o status HTTP definido pelo {@code statusCode}. Ele **nunca retorna normalmente**, portanto
     * chamadas subsequentes ao método não serão executadas.
     *
     * @param level      Nível de log a ser usado (DEBUG, INFO, WARN, ERROR)
     * @param msgLog        Mensagem de log. Pode conter placeholders "{}" que serão substituídos por {@code args}.
     * @param msgThrow   Mensagem a ser passada para a {@link StorageException} lançada.
     * @param statusCode Código HTTP que será usado na exceção lançada.
     * @param args       Argumentos opcionais para formatar a {@code msg}.
     *
     * @throws StorageException Sempre lançada, com a {@code msgThrow} como mensagem e {@code statusCode}.
     *
     * <pre>{@code
     * @Contract("_, _, _, _, _ -> fail")
     * }</pre>
     *
     * <p>Explicação do contrato:
     * <ul>
     *   <li>Cada '_' representa que o argumento naquela posição pode ser qualquer valor.</li>
     *   <li>Neste caso, temos cinco argumentos: (level, msg, msgThrow, statusCode, args):
     *       <ul>
     *           <li>1º argumento (_) = level: qualquer nível de log permitido</li>
     *           <li>2º argumento (_) = msg: qualquer String de log</li>
     *           <li>3º argumento (_) = msgThrow: qualquer String de mensagem de exceção</li>
     *           <li>4º argumento (_) = statusCode: qualquer código HTTP</li>
     *           <li>5º argumento (_) = args: qualquer array de objetos para formatação</li>
     *       </ul>
     *   </li>
     *   <li>'-> fail' indica que o método <b>sempre lança uma exceção</b>, independentemente dos valores fornecidos.</li>
     * </ul>
     *
     * <p>Objetivo:
     * <ul>
     *   <li>Auxiliar ferramentas de análise estática a identificar que este método nunca retorna.</li>
     *   <li>Evitar avisos de código inalcançável ou possíveis NullPointerExceptions em chamadas subsequentes.</li>
     *   <li>Documentar claramente que a execução é interrompida pela exceção {@link StorageException}.</li>
     * </ul>
     */
    @Contract("_, _, _, _, _ -> fail")
    public static void logAndThrow(Level level, String msgLog, String msgThrow, int statusCode, Object... args){
        log(level, msgLog, args);
        throw new StorageException(msgThrow,  statusCode);
    }

    /**
     * Lança uma exceção de armazenamento após registrar uma mensagem de log no nível especificado.
     * <p>
     * Este método registra a mensagem usando o nível fornecido e então lança uma {@link StorageException}
     * com o status HTTP definido pelo {@code statusCode}. Ele **nunca retorna normalmente**, portanto
     * chamadas subsequentes ao método não serão executadas.
     *
     * @param level      Nível de log a ser usado (DEBUG, INFO, WARN, ERROR)
     * @param msgLog        Mensagem de log. Pode conter placeholders "{}" que serão substituídos por {@code args}.
     * @param msgThrow   Mensagem a ser passada para a {@link StorageException} lançada.
     * @param statusCode Código HTTP que será usado na exceção lançada.
     * @param e          Exceção causa original (opcional, pode ser {@code null}) que será encapsulada na {@link StorageException}.
     * @param args       Argumentos opcionais para formatar a {@code msg}.
     *
     * @throws StorageException Sempre lançada, com a {@code msgThrow} como mensagem e {@code statusCode}.
     *
     * <pre>{@code
     * @Contract("_, _, _, _, _ -> fail")
     * }</pre>
     *
     * <p>Explicação do contrato:
     * <ul>
     *   <li>Cada '_' representa que o argumento naquela posição pode ser qualquer valor.</li>
     *   <li>Neste caso, temos cinco argumentos: (level, msg, msgThrow, statusCode, args):
     *       <ul>
     *           <li>1º argumento (_) = level: qualquer nível de log permitido</li>
     *           <li>2º argumento (_) = msg: qualquer String de log</li>
     *           <li>3º argumento (_) = msgThrow: qualquer String de mensagem de exceção</li>
     *           <li>4º argumento (_) = statusCode: qualquer código HTTP</li>
     *           <li>5º argumento (_) = e: exceção causa original (Throwable), pode ser null</li>
     *           <li>6º argumento (_) = args: qualquer array de objetos para formatação</li>
     *       </ul>
     *   </li>
     *   <li>'-> fail' indica que o método <b>sempre lança uma exceção</b>, independentemente dos valores fornecidos.</li>
     * </ul>
     *
     * <p>Objetivo:
     * <ul>
     *   <li>Auxiliar ferramentas de análise estática a identificar que este método nunca retorna.</li>
     *   <li>Evitar avisos de código inalcançável ou possíveis NullPointerExceptions em chamadas subsequentes.</li>
     *   <li>Documentar claramente que a execução é interrompida pela exceção {@link StorageException}.</li>
     * </ul>
     */
    @Contract("_, _, _, _, _, _ -> fail")
    public static void logAndThrow(Level level, String msgLog, String msgThrow, int statusCode, Throwable e, Object... args){
        log(level, msgLog, args);
        throw new StorageException(msgThrow,  statusCode, e);
    }

    public static StorageException logAndCreateException(Level level, String msgLog, String msgThrow, int statusCode, Throwable e, Object... args){
        log(level, msgLog, args);
        return new StorageException(msgThrow,  statusCode, e);
    }

    private static void log(Level level, String msgLog, Object... args) {
        switch (level) {
            case ERROR -> logger.error(msgLog, args);
            case WARN -> logger.warn(msgLog, args);
            case INFO -> logger.info(msgLog, args);
            case DEBUG -> logger.debug(msgLog, args);
            case TRACE -> logger.trace(msgLog, args);
        }
    }
}