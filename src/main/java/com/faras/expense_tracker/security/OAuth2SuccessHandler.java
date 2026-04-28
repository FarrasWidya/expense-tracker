package com.faras.expense_tracker.security;

import com.faras.expense_tracker.entity.User;
import com.faras.expense_tracker.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    public OAuth2SuccessHandler(UserRepository userRepository, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String googleId = oAuth2User.getAttribute("sub");

        User user = userRepository.findByGoogleId(googleId)
                .orElseGet(() -> userRepository.findByEmail(email)
                        .map(existing -> {
                            existing.setGoogleId(googleId);
                            existing.setProvider(User.Provider.GOOGLE);
                            return userRepository.save(existing);
                        })
                        .orElseGet(() -> {
                            User newUser = new User();
                            newUser.setEmail(email);
                            newUser.setGoogleId(googleId);
                            newUser.setProvider(User.Provider.GOOGLE);
                            return userRepository.save(newUser);
                        }));

        String token = jwtUtil.generateToken(user.getId());
        String destination = "/";
        jakarta.servlet.http.Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (jakarta.servlet.http.Cookie c : cookies) {
                if ("catetu_origin".equals(c.getName()) && "desktop".equals(c.getValue())) {
                    destination = "/desktop.html";
                    jakarta.servlet.http.Cookie clear = new jakarta.servlet.http.Cookie("catetu_origin", "");
                    clear.setMaxAge(0);
                    clear.setPath("/");
                    response.addCookie(clear);
                    break;
                }
            }
        }
        getRedirectStrategy().sendRedirect(request, response, destination + "?token=" + token);
    }
}
