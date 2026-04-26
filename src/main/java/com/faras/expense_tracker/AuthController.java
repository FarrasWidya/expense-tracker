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
    public record UserResponse(Long id, String email, String provider, String name, String avatarInitials, boolean hasAvatar) {}
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
        String initials = deriveInitials(user.getName() != null ? user.getName() : user.getEmail());
        return new UserResponse(user.getId(), user.getEmail(), user.getProvider().name(),
                user.getName(), initials, user.getAvatarData() != null);
    }

    private static String deriveInitials(String value) {
        if (value == null || value.isBlank()) return "?";
        String[] parts = value.trim().split("[@\\s]+");
        if (parts.length >= 2) return (parts[0].charAt(0) + "" + parts[1].charAt(0)).toUpperCase();
        return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
    }
}
