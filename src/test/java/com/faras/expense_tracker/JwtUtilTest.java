package com.faras.expense_tracker;

import com.faras.expense_tracker.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class JwtUtilTest {

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    void generateAndExtract_roundTrip() {
        String token = jwtUtil.generateToken(42L);
        assertNotNull(token);
        assertEquals(42L, jwtUtil.extractUserId(token));
        assertTrue(jwtUtil.isValid(token));
    }

    @Test
    void differentUsers_getDifferentTokens() {
        String t1 = jwtUtil.generateToken(1L);
        String t2 = jwtUtil.generateToken(2L);
        assertNotEquals(t1, t2);
        assertEquals(1L, jwtUtil.extractUserId(t1));
        assertEquals(2L, jwtUtil.extractUserId(t2));
    }

    @Test
    void invalidToken_isInvalid() {
        assertFalse(jwtUtil.isValid("not.a.valid.token"));
    }

    @Test
    void tamperedToken_isInvalid() {
        String token = jwtUtil.generateToken(1L);
        String tampered = token.substring(0, token.length() - 3) + "XYZ";
        assertFalse(jwtUtil.isValid(tampered));
    }
}
