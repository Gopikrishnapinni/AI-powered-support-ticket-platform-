package com.example.support;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class SupportTicketApplication {
    public static void main(String[] args) {
        SpringApplication.run(SupportTicketApplication.class, args);
    }
}
