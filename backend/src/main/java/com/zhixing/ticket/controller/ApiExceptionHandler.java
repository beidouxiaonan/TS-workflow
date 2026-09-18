package com.zhixing.ticket.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.validation.ConstraintViolationException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String,Object>> forbidden(SecurityException e){return response(HttpStatus.FORBIDDEN,e.getMessage());}
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String,Object>> notFound(IllegalArgumentException e){return response(HttpStatus.NOT_FOUND,e.getMessage());}
    @ExceptionHandler({IllegalStateException.class,MethodArgumentNotValidException.class,ConstraintViolationException.class})
    public ResponseEntity<Map<String,Object>> badRequest(Exception e){return response(HttpStatus.BAD_REQUEST,e.getMessage());}
    private ResponseEntity<Map<String,Object>> response(HttpStatus status,String message){Map<String,Object> body=new LinkedHashMap<String,Object>();body.put("timestamp",new Date());body.put("status",status.value());body.put("error",status.getReasonPhrase());body.put("message",message);return ResponseEntity.status(status).body(body);}
}
