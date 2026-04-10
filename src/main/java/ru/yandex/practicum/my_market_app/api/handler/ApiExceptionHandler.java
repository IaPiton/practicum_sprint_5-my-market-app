package ru.yandex.practicum.my_market_app.api.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.yandex.practicum.my_market_app.api.model.ErrorResponse;

@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(CartNotFoundException.class)
    public ResponseEntity<ErrorResponse> cartNotFoundException(CartNotFoundException e) {
        log.error("❌ Ошибка: корзина не найдена");
        logError(e);
        log.error("📚 Стек ошибки:", e);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("BAD_REQUEST", e.getMessage()));
    }

    @ExceptionHandler(ItemNotFoundException.class)
    public ResponseEntity<ErrorResponse> itemNotFoundException(ItemNotFoundException e) {
        log.error("❌ Ошибка: товар не найден");
        logError(e);
        log.error("📚 Стек ошибки:", e);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("BAD_REQUEST", e.getMessage()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> itemNotFoundException(MissingServletRequestParameterException e) {
        log.error("❌ Ошибка: входящих параметров");
        logError(e);
        log.error("📚 Стек ошибки:", e);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("BAD_REQUEST", e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleInternalError(Exception e) {
        log.error("💥 Критическая ошибка приложения");
        log.error("📝 Тип ошибки: {}", e.getClass().getSimpleName());
        logError(e);
        log.error("📚 Полный стек ошибки:", e);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_SERVER", "Произошла непредвиденная ошибка"));
    }

    private void logError(Exception e) {
        log.error("📝 Сообщение: {}", e.getMessage());
    }

}