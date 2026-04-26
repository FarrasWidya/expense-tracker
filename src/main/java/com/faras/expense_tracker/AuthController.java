package com.faras.expense_tracker;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    @Value("${GOOGLE_CLIENT_ID:dummy-local-client-id}")
    private String googleClientId;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    public record RegisterRequest(String email, String password) {}
    public record LoginRequest(String email, String password) {}
    public record TokenResponse(String token) {}
    public record UserResponse(Long id, String email, String provider) {}
    public record ConfigResponse(boolean googleEnabled) {}

    @GetMapping("/config")
    public ConfigResponse config() {
        return new ConfigResponse(!"dummy-local-client-id".equals(googleClientId));
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenResponse register(@RequestBody RegisterRequest req) {
        return new TokenResponse(authService.register(req.email(), req.password()));
    }

    @PostMapping("/login")
    public TokenResponse login(@RequestBody LoginRequest req) {
        return new TokenResponse(authService.login(req.email(), req.password()));
    }

    @GetMapping("/me")
    public UserResponse me(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED);
        }
        Long userId = (Long) auth.getPrincipal();
        User user = authService.getUser(userId);
        return new UserResponse(user.getId(), user.getEmail(), user.getProvider().name());
    }
}
