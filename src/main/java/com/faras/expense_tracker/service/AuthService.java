package com.faras.expense_tracker.service;

import com.faras.expense_tracker.entity.User;
import com.faras.expense_tracker.repository.UserRepository;
import com.faras.expense_tracker.security.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public String register(String email, String rawPassword) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setProvider(User.Provider.LOCAL);
        User saved = userRepository.save(user);
        return jwtUtil.generateToken(saved.getId());
    }

    public String login(String email, String rawPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        if (user.getPassword() == null || !passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        return jwtUtil.generateToken(user.getId());
    }

    public User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @org.springframework.transaction.annotation.Transactional
    public User updateProfile(UUID userId, String name) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        user.setName(name != null ? name.trim() : null);
        return userRepository.save(user);
    }

    @org.springframework.transaction.annotation.Transactional
    public User saveAvatar(UUID userId, String base64DataUri) {
        if (base64DataUri != null && base64DataUri.length() > 700_000) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Avatar must be under 500KB");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        user.setAvatarData(base64DataUri);
        return userRepository.save(user);
    }
}
