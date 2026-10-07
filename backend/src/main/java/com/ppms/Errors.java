package com.ppms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import java.util.Map;

/** Turns every failure into a short, user-friendly JSON message (never a stack trace). */
@RestControllerAdvice
public class Errors {
    private static final Logger log = LoggerFactory.getLogger(Errors.class);

    public static class ApiException extends RuntimeException {
        final int status;
        public ApiException(int status, String message) { super(message); this.status = status; }
    }
    private static ResponseEntity<Map<String, Object>> r(int s, String m) { return ResponseEntity.status(s).body(Map.of("message", m)); }

    @ExceptionHandler(ApiException.class) ResponseEntity<Map<String, Object>> api(ApiException e) { return r(e.status, e.getMessage()); }
    @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<Map<String, Object>> valid(MethodArgumentNotValidException e) {
        var fe = e.getBindingResult().getFieldErrors().get(0);
        return r(400, "Please check '" + fe.getField() + "': " + fe.getDefaultMessage());
    }
    @ExceptionHandler(HttpMessageNotReadableException.class) ResponseEntity<Map<String, Object>> bad(Exception e) { return r(400, "Some of the details you entered are not valid."); }
    @ExceptionHandler(DataIntegrityViolationException.class) ResponseEntity<Map<String, Object>> dup(Exception e) { log.warn("Integrity violation", e); return r(409, "Booking could not be completed. Please try again."); }
    @ExceptionHandler(AccessDeniedException.class) ResponseEntity<Map<String, Object>> denied(Exception e) { return r(403, "You are not allowed to do this."); }
    @ExceptionHandler(NoResourceFoundException.class) ResponseEntity<Map<String, Object>> nf(Exception e) { return r(404, "Not found."); }
    @ExceptionHandler(Exception.class) ResponseEntity<Map<String, Object>> other(Exception e) { log.error("Unexpected error", e); return r(500, "Something went wrong. Please try again."); }
}
