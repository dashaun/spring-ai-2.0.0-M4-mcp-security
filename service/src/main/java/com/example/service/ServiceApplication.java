package com.example.service;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

@SpringBootApplication
public class ServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceApplication.class, args);
    }

}

@Service
class SchedulerService {

    @McpTool(description = "schedule an appointment to pick up " +
            "or adopt a dog from a Pooch Palace location")
    DogAdoptionSchedule scheduleAdoption(@McpToolParam int dogId,
                                         @McpToolParam String dogName) {
        var user = Objects.requireNonNull(SecurityContextHolder
                        .getContext()
                        .getAuthentication())
                .getName();
        var das = new DogAdoptionSchedule(Instant
                .now()
                .plus(3, ChronoUnit.DAYS), user);
        IO.println("das: " + das + " dogId: " + dogId + " dogName: " + dogName);
        return das;
    }
}

record DogAdoptionSchedule(Instant when, String user) {
}