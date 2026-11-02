package com.finpilot.core.auth;

import com.finpilot.core.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "Administration of team members and access roles")
public class UserController {

    private final UserRepository userRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR')")
    @Operation(summary = "List all platform users with roles and status")
    public ResponseEntity<ApiResponse<List<AuthDtos.UserDto>>> getAllUsers() {
        List<AuthDtos.UserDto> users = userRepository.findAll().stream()
                .map(AuthDtos.UserDto::fromEntity)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(users));
    }
}
