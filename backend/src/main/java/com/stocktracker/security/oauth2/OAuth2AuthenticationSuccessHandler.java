package com.stocktracker.security.oauth2;

import com.stocktracker.entity.User;
import com.stocktracker.repository.UserRepository;
import com.stocktracker.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;

    @Value("${app.oauth2.redirectUri:http://localhost:3000/oauth2/redirect}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        if (response.isCommitted()) {
            return;
        }

        // Get the OAuth2User from authentication
        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        
        // Extract email from OAuth2User attributes
        String email = oauth2User.getAttribute("email");
        
        if (email == null) {
            throw new IllegalStateException("Email not found in OAuth2 user attributes");
        }
        
        // Load user from database
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalStateException("User not found after OAuth2 authentication"));
        
        // Generate JWT token
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        String token = jwtTokenProvider.generateToken(userDetails);

        // Build redirect URL with user info
        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
            .queryParam("token", token)
            .queryParam("userId", user.getId())
            .queryParam("email", user.getEmail())
            .queryParam("name", user.getName())
            .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
