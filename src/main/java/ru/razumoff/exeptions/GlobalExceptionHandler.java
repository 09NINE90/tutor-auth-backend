package ru.razumoff.exeptions;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.razumoff.exceptions.ApiError;
import ru.razumoff.exceptions.ErrorCode;
import ru.razumoff.exceptions.PlatformException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PlatformException.class)
    public ResponseEntity<ApiError> handlePlatformException(PlatformException ex,
                                                            HttpServletRequest request) {
        log.warn("Platform exception occurred: {} (code: {})", ex.getCustomMessage(), ex.getErrorCode(), ex);

        ErrorCode code = ex.getErrorCode();
        String message = ex.getCustomMessage() != null
                ? ex.getCustomMessage()
                : code.getDefaultMessage();

        ApiError error = new ApiError(
                Instant.now(),
                code.getHttpStatus(),
                code.getCode(),
                message,
                request.getRequestURI()
        );

        return ResponseEntity.status(code.getHttpStatus()).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex,
                                                  HttpServletRequest request) {
        log.error("Unhandled generic exception occurred at URI: {}", request.getRequestURI(), ex);

        ApiError error = new ApiError(
                Instant.now(),
                500,
                ErrorCode.INTERNAL_ERROR.getCode(),
                "Internal server error",
                request.getRequestURI()
        );
        return ResponseEntity.internalServerError().body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        log.warn("Validation errors occurred", ex);
        ApiError error = new ApiError(
                Instant.now(),
                400,
                ErrorCode.VALIDATION_ERROR.getCode(),
                ErrorCode.VALIDATION_ERROR.getDefaultMessage(),
                null
        );

        Map<String, List<String>> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.groupingBy(
                        FieldError::getField,
                        Collectors.mapping(FieldError::getDefaultMessage, Collectors.toList())
                ));

        error.setFieldErrors(fieldErrors);

        return ResponseEntity.badRequest().body(error);
    }

}

