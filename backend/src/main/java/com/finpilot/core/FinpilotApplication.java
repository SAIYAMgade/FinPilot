package com.finpilot.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = {
    // Exclude strict startup failures if external Redis or Kafka are absent during local standalone dev
    RedisAutoConfiguration.class
})
@EnableAsync
@EnableScheduling
public class FinpilotApplication {
    public static void main(String[] args) {
        SpringApplication.run(FinpilotApplication.class, args);
    }
}
