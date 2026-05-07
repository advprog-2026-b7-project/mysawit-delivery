package id.ac.ui.cs.advprog.mysawit.delivery.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final String ERROR_KEY = "error";
    private static final String MESSAGE_KEY = "message";

    // 1. Menangkap error kalau data tidak ditemukan (misal dari findById)
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NoSuchElementException ex) {
        Map<String, String> response = new HashMap<>();
        response.put(ERROR_KEY, "Data Tidak Ditemukan");
        response.put(MESSAGE_KEY, ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response); // Mengembalikan 404
    }

    // 2. Menangkap error kalau ada aturan bisnis yang dilanggar (misal status salah)
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException ex) {
        Map<String, String> response = new HashMap<>();
        response.put(ERROR_KEY, "Konflik Status");
        response.put(MESSAGE_KEY, ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response); // Mengembalikan 409
    }

    // 3. Menangkap error kalau input tidak valid (misal parameter kosong)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        Map<String, String> response = new HashMap<>();
        response.put(ERROR_KEY, "Input Tidak Valid");
        response.put(MESSAGE_KEY, ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response); // Mengembalikan 400
    }
}