package com.mbank.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
public class PingController {

    @GetMapping("/ping")
    public Map<String, Object> ping() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "OK");
        response.put("service", "MBank Spring Boot");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("message", "Service is awake and running");
        return response;
    }

    @GetMapping("/")
    public String home() {
        return "MBank Spring Boot is running! Go to /login to start.";
    }
}