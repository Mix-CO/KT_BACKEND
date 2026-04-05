package com.kicktime.backend.domain.services.authentication;

import com.kicktime.backend.domain.model.User;
import com.kicktime.backend.domain.model.enums.UserRole;
import com.kicktime.backend.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String email = oAuth2User.getAttribute("email");
        String name  = oAuth2User.getAttribute("name");

        // Si el usuario no existe en tu BD, lo registra automáticamente
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .email(email)
                                .name(name)
                                .role(UserRole.PLAYER)
                                .build()
                ));

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());

        // Redirige al frontend con el token como query param
        response.sendRedirect("http://localhost:5173/oauth2/callback?token=" + token);
    }
}