package com.example.service;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

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
    DogAdoptionSchedule scheduleAdoption(@McpToolParam(description = "Dog ID", required = false) Integer dogId,
                                         @McpToolParam(description = "Dog Name", required = false) String dogName) {
        int id = (dogId != null) ? dogId : ThreadLocalRandom.current().nextInt(1, 101);
        List<String> goodNames = List.of("Fluffy", "Prancer", "Spot", "Butters");
        String name = (dogName != null) ? dogName : goodNames.get(ThreadLocalRandom.current().nextInt(goodNames.size()));

        var user = Objects.requireNonNull(SecurityContextHolder
                        .getContext()
                        .getAuthentication())
                .getName();
        var das = new DogAdoptionSchedule(Instant
                .now()
                .plus(3, ChronoUnit.DAYS), user);
        IO.println("das: " + das + " dogId: " + id + " dogName: " + name);
        return das;
    }
}

record DogAdoptionSchedule(Instant when, String user) {
}