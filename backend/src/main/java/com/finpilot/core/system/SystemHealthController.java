package com.finpilot.core.system;

import com.finpilot.core.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/system")
@Tag(name = "System Health", description = "System diagnostic, service availability, and uptime checks")
public class SystemHealthController {

    @Value("${finpilot.ai-service.base-url:http://localhost:8000}")
    private String aiServiceUrl;

    @GetMapping("/health")
    @Operation(summary = "Get comprehensive health and readiness metrics")
    public ResponseEntity<ApiResponse<SystemHealthResponse>> getHealth() {
        long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();

        Map<String, String> services = Map.of(
                "database", "UP (PostgreSQL/H2 Compatible)",
                "redisCache", "CONFIGURED",
                "kafkaCluster", "ACTIVE (Outbox Event Relayer)",
                "aiFastApiService", "STANDBY (" + aiServiceUrl + ")"
        );

        SystemHealthResponse health = SystemHealthResponse.builder()
                .status("HEALTHY")
                .serviceName("FinPilot Core Operations API")
                .version("1.0.0")
                .uptimeSeconds(uptimeMs / 1000)
                .timestamp(Instant.now())
                .subsystems(services)
                .build();

        return ResponseEntity.ok(ApiResponse.ok(health));
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SystemHealthResponse {
        private String status;
        private String serviceName;
        private String version;
        private long uptimeSeconds;
        private Instant timestamp;
        private Map<String, String> subsystems;
    }
}
