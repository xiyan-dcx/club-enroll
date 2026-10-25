package com.club.config;
import com.club.util.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.dao.DuplicateKeyException;
@RestControllerAdvice
public class ApiErrors {
 @ExceptionHandler({IllegalArgumentException.class,ArithmeticException.class,DuplicateKeyException.class})
 public ResponseEntity<?> bad(Exception e){return ResponseEntity.badRequest().body(ApiResponse.fail(400,e instanceof DuplicateKeyException?"记录已存在":e.getMessage()));}
 @ExceptionHandler(SecurityException.class) public ResponseEntity<?> denied(Exception e){return ResponseEntity.status(403).body(ApiResponse.fail(403,e.getMessage()));}
 @ExceptionHandler(Busy.class) public ResponseEntity<?> busy(Busy e){return ResponseEntity.status(e.code).body(ApiResponse.fail(e.code,e.getMessage()));}
 public static class Busy extends RuntimeException {public final int code;public Busy(int code,String text){super(text);this.code=code;}}
}
