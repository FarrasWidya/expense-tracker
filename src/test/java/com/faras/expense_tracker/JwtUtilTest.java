package com.faras.expense_tracker;

import com.faras.expense_tracker.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class JwtUtilTest {

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    void generateAndExtract_roundTrip() {
        UUID id = UUID.randomUUID();
        String token = jwtUtil.generateToken(id);
        assertNotNull(token);
        assertEquals(id, jwtUtil.extractUserId(token));
        assertTrue(jwtUtil.isValid(token));
    }

    @Test
    void differentUsers_getDifferentTokens() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        String t1 = jwtUtil.generateToken(id1);
        String t2 = jwtUtil.generateToken(id2);
        assertNotEquals(t1, t2);
        assertEquals(id1, jwtUtil.extractUserId(t1));
        assertEquals(id2, jwtUtil.extractUserId(t2));
    }

    @Test
    void invalidToken_isInvalid() {
        assertFalse(jwtUtil.isValid("not.a.valid.token"));
    }

    @Test
    void tamperedToken_isInvalid() {
        String token = jwtUtil.generateToken(UUID.randomUUID());
        String tampered = token.substring(0, token.length() - 3) + "XYZ";
        assertFalse(jwtUtil.isValid(tampered));
    }
}
