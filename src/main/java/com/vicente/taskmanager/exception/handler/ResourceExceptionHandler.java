package com.vicente.taskmanager.exception.handler;

import com.vicente.taskmanager.exception.InvalidTaskStatusException;
import com.vicente.taskmanager.exception.TaskStatusNotAllowedException;
import com.vicente.taskmanager.exception.error.StandardError;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.stream.Collectors;
@RestControllerAdvice
public class ResourceExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<StandardError> entityNotFound(EntityNotFoundException e, HttpServletRequest request) {
        String error = "Entity Not Found Error";
        HttpStatus status = HttpStatus.NOT_FOUND;
        StandardError standardError = new StandardError(Instant.now(),status.value(), error,
                e.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(standardError);
    }

    @ExceptionHandler(TaskStatusNotAllowedException.class)
    public ResponseEntity<StandardError> taskStatusNotAllowed(TaskStatusNotAllowedException e, HttpServletRequest request) {
        String error = "Task Status Not Allowed Error";
        HttpStatus status = HttpStatus.UNPROCESSABLE_CONTENT;
        StandardError standardError = new StandardError(Instant.now(),status.value(), error,
                e.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(standardError);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<StandardError> methodArgumentNotValid(MethodArgumentNotValidException e, HttpServletRequest request) {
        String error = "Method Argument Not Valid Error";
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String message = buildValidationMessage(e);

        StandardError standardError = new StandardError(Instant.now(),status.value(), error,
                message, request.getRequestURI());
        return ResponseEntity.status(status).body(standardError);
    }

    @ExceptionHandler(InvalidTaskStatusException.class)
    public ResponseEntity<StandardError> invalidTaskStatus(InvalidTaskStatusException e, HttpServletRequest request) {
        String error = "Invalid Task Status Error";
        HttpStatus status = HttpStatus.BAD_REQUEST;
        StandardError standardError = new StandardError(Instant.now(),status.value(), error,
                e.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(standardError);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<StandardError> methodArgumentTypeMismatch(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        String error = "Method Argument Type Mismatch Error";
        HttpStatus status = HttpStatus.BAD_REQUEST;

        String message = String.format("Invalid value '%s' for parameter '%s'", e.getValue(), e.getName());

        StandardError standardError = new StandardError(Instant.now(),status.value(), error,
                message, request.getRequestURI());
        return ResponseEntity.status(status).body(standardError);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<StandardError> httpMessageNotReadable(HttpMessageNotReadableException e, HttpServletRequest request) {
        String error = "Http Message Not Readable Error";
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String message = "Malformed JSON request";

        Throwable rootCause = e.getCause();
        while (rootCause != null && !(rootCause instanceof DateTimeParseException)) {
            rootCause = rootCause.getCause();
        }
        if (rootCause instanceof DateTimeParseException dtpe) {
            message = dtpe.getParsedString() + " is not a valid calendar date";
        }


        StandardError standardError = new StandardError(Instant.now(),status.value(), error,
                message, request.getRequestURI());
        return ResponseEntity.status(status).body(standardError);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<StandardError> handleGeneric(Exception e, HttpServletRequest request) {
        String error = "Internal Server Error";
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String message = "Unexpected error occurred. Please contact support.";
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
}

