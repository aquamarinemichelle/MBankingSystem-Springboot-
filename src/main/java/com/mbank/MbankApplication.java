package com.mbank;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MbankApplication {

    public static void main(String[] args) {
        SpringApplication.run(MbankApplication.class, args);
        System.out.println(" MBank Spring Boot Started!");
        System.out.println(" http://localhost:8080");
        System.out.println("http://localhost:8080/login");
    }
}