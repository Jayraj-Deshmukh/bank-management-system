package com.bank.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> checkHealth() {
        Map<String, Object> status = Map.of(
                "status", "UP",
                "service", "Bank Management System API",
                "timestamp", LocalDateTime.now().toString()
        );
        return ResponseEntity.ok(status);
    }
}
