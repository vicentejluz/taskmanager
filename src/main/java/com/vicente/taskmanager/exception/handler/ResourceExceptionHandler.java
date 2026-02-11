package com.vicente.taskmanager.exception.handler;

import com.vicente.taskmanager.exception.InvalidTaskStatusException;
import com.vicente.taskmanager.exception.TaskStatusNotAllowedException;
import com.vicente.taskmanager.exception.error.StandardError;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ResourceExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(ResourceExceptionHandler.class);

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<StandardError> entityNotFound(EntityNotFoundException e, HttpServletRequest request) {
        String error = "Entity Not Found Error";
        HttpStatus status = HttpStatus.NOT_FOUND;

        logExceptionWarn(error, status, request, e.getMessage());

        StandardError standardError = new StandardError(Instant.now(),status.value(), error,
                e.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(standardError);
    }

    @ExceptionHandler(TaskStatusNotAllowedException.class)
    public ResponseEntity<StandardError> taskStatusNotAllowed(TaskStatusNotAllowedException e, HttpServletRequest request) {
        String error = "Task Status Not Allowed Error";
        HttpStatus status = HttpStatus.UNPROCESSABLE_CONTENT;

        logExceptionWarn(error, status, request, e.getMessage());

        StandardError standardError = new StandardError(Instant.now(),status.value(), error,
                e.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(standardError);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<StandardError> methodArgumentNotValid(MethodArgumentNotValidException e, HttpServletRequest request) {
        String error = "Method Argument Not Valid Error";
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String message = buildValidationMessage(e);

        logExceptionWarn(error, status, request, message);

        StandardError standardError = new StandardError(Instant.now(),status.value(), error,
                message, request.getRequestURI());
        return ResponseEntity.status(status).body(standardError);
    }

    @ExceptionHandler(InvalidTaskStatusException.class)
    public ResponseEntity<StandardError> invalidTaskStatus(InvalidTaskStatusException e, HttpServletRequest request) {
        String error = "Invalid Task Status Error";
        HttpStatus status = HttpStatus.BAD_REQUEST;

        logExceptionWarn(error, status, request, e.getMessage());

        StandardError standardError = new StandardError(Instant.now(),status.value(), error,
                e.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(standardError);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<StandardError> methodArgumentTypeMismatch(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        String error = "Method Argument Type Mismatch Error";
        HttpStatus status = HttpStatus.BAD_REQUEST;

        String message = String.format("Invalid value '%s' for parameter '%s'", e.getValue(), e.getName());

        logExceptionWarn(error, status, request, message);

        StandardError standardError = new StandardError(Instant.now(),status.value(), error,
                message, request.getRequestURI());
        return ResponseEntity.status(status).body(standardError);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<StandardError> httpMessageNotReadable(HttpMessageNotReadableException e, HttpServletRequest request) {
        String error = "Http Message Not Readable Error";
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String message = "Malformed JSON request";


        /*
        Esse bloco de código é responsável por identificar a CAUSA REAL da exceção
        HttpMessageNotReadableException quando o erro ocorre durante a leitura do JSON.

        Em muitos casos, essa exceção é apenas "genérica" e encapsula outras exceções
        mais específicas dentro dela (exceções encadeadas / nested exceptions).

        Exemplo comum:
            - O cliente envia uma data inválida no JSON (ex: "2025-02-31")
            - O Jackson tenta converter essa String para LocalDate
            - O parse falha e gera um DateTimeParseException
            - Essa exceção é encapsulada dentro de outras exceções
            - No final, o Spring lança apenas HttpMessageNotReadableException

        Para conseguir uma mensagem de erro mais clara para o cliente,
        precisamos "descer" na cadeia de causas até encontrar o erro original.
        */

        /*
        Começamos pegando a causa imediata da exceção HttpMessageNotReadableException.
        O método getCause() retorna a exceção que causou essa exceção atual.
        */
        Throwable rootCause = e.getCause();

        /*
        Percorremos a cadeia de exceções usando um loop while.
        A ideia aqui é caminhar pelas causas encadeadas (getCause())
        até encontrar uma exceção do tipo DateTimeParseException.

        Condições do loop:
            - rootCause != null → evita NullPointerException
            - !(rootCause instanceof DateTimeParseException) →
                continua o loop enquanto a exceção atual NÃO for do tipo desejado
        */
        while (rootCause != null && !(rootCause instanceof DateTimeParseException)) {
            /*
            Avança para a próxima causa na cadeia de exceções.
            Isso permite "desembrulhar" exceções até chegar na raiz do problema.
            */
            rootCause = rootCause.getCause();
        }

        /*
        Após sair do loop, verificamos se a exceção encontrada
        realmente é uma DateTimeParseException.

        Caso seja:
            - Usamos pattern matching do Java (instanceof ... dtpe)
            - Extraímos a String que falhou no parse (dtpe.getParsedString())
            - Montamos uma mensagem de erro mais clara e amigável para o consumidor da API
        */
        if (rootCause instanceof DateTimeParseException dtpe) {
            message = dtpe.getParsedString() + " is not a valid calendar date";
        }

        logExceptionWarn(error, status, request, message);

        StandardError standardError = new StandardError(Instant.now(),status.value(), error,
                message, request.getRequestURI());
        return ResponseEntity.status(status).body(standardError);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<StandardError> httpMediaTypeNotSupported(HttpMediaTypeNotSupportedException e, HttpServletRequest request){
        String error = "Http Media Type Not Supported Error";
        HttpStatus status = HttpStatus.UNSUPPORTED_MEDIA_TYPE;

        logger.warn("{} | status={} method={} path={} contentType={} supported={} message={}", error, status.value(),
                request.getMethod(), request.getRequestURI(), request.getContentType(), e.getSupportedMediaTypes(),
                e.getMessage());

        StandardError standardError = new StandardError(Instant.now(),status.value(), error,
                e.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(standardError);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<StandardError> noResourceFound(NoResourceFoundException e, HttpServletRequest request){
        String error = "No Resource Found Error";
        HttpStatus status = HttpStatus.NOT_FOUND;

        logExceptionWarn(error, status, request, e.getMessage());

        StandardError standardError = new StandardError(Instant.now(),status.value(), error,
                e.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(standardError);
    }

    @ExceptionHandler({ OptimisticLockException.class, ObjectOptimisticLockingFailureException.class })
    public ResponseEntity<StandardError> optimisticLock(Exception e, HttpServletRequest request) {
        String error = "Optimistic Lock Error";
        HttpStatus status = HttpStatus.CONFLICT;
        String message = "Concurrent update detected. Please reload the resource and retry the operation.";

        logExceptionWarn(error, status, request, message);

        StandardError standardError = new StandardError(
                Instant.now(),
                status.value(),
                error,
                message,
                request.getRequestURI()
        );

        return ResponseEntity.status(status).body(standardError);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<StandardError> handleGeneric(Exception e, HttpServletRequest request) {
        String error = "Internal Server Error";
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String message = "Unexpected error occurred. Please contact support.";

        logger.error("{} | status={} method={} path={} message={}", error, status.value(), request.getMethod(),
                request.getRequestURI(), message, e);

        StandardError standardError = new StandardError(Instant.now(),status.value(), error,
                message, request.getRequestURI());
        return ResponseEntity.status(status).body(standardError);
    }


    private String buildValidationMessage(MethodArgumentNotValidException e) {
        /*
            Converte a lista de FieldError em uma mensagem única:
            Percorre todos os erros de validação dos campos (@Valid),
            transforma cada erro no formato "campo: mensagem",
            remove mensagens duplicadas (caso o mesmo erro apareça mais de uma vez)
            e junta tudo em uma única String separada por "; ".
            Exemplo de saída:
                "dueDate: must be present or future; title: must not be blank"

            - stream(): percorre os erros de validação
            - map(): formata cada erro como "campo: mensagem"
            - distinct(): evita mensagens duplicadas
            - joining(): concatena tudo em uma única String separada por "; "
        */
        return e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .distinct()
                .collect(Collectors.joining("; "));
    }

    private void logExceptionWarn(String error, HttpStatus status, HttpServletRequest request, String message) {
        logger.warn("{} | status={} method={} path={} message={}", error, status.value(), request.getMethod(),
                request.getRequestURI(), message);
    }
}

