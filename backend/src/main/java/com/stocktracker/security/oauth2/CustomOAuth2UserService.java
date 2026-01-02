package com.stocktracker.security.oauth2;

import com.stocktracker.entity.User;
import com.stocktracker.entity.User.AuthProvider;
import com.stocktracker.exception.BadRequestException;
import com.stocktracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

    private final UserRepository userRepository;
    private final OidcUserService delegate = new OidcUserService();

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        // Delegate to the default OIDC user service to load the user
        OidcUser oidcUser = delegate.loadUser(userRequest);
        
        // Process and save/update the user in our database
        processOAuth2User(userRequest, oidcUser);
        
        // Return the OIDC user as-is (Spring Security will handle it correctly)
        return oidcUser;
    }

    private void processOAuth2User(OidcUserRequest userRequest, OidcUser oidcUser) {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        
        // Extract user info from OIDC user
        String email = oidcUser.getEmail();
        String name = oidcUser.getFullName();
        String providerId = oidcUser.getSubject();
        String imageUrl = oidcUser.getAttribute("picture");

        if (email == null || email.isEmpty()) {
            throw new BadRequestException("Email not found from OAuth2 provider");
        }

        log.info("Processing OAuth2 user: email={}, name={}, provider={}", email, name, registrationId);

        Optional<User> existingUser = userRepository.findByEmail(email);

        if (existingUser.isPresent()) {
            User user = existingUser.get();
            // Check if user registered with different provider
            if (!user.getAuthProvider().name().equalsIgnoreCase(registrationId)) {
                throw new BadRequestException(
                    "You're already signed up with " + user.getAuthProvider() + 
                    ". Please use your " + user.getAuthProvider() + " account to login."
                );
            }
            // Update existing user info
            updateExistingUser(user, name, imageUrl);
            log.info("Updated existing user: id={}, email={}", user.getId(), email);
        } else {
            // Register new user
            User newUser = registerNewUser(registrationId, email, name, providerId, imageUrl);
            log.info("Created new user: id={}, email={}", newUser.getId(), email);
        }
    }

    private User registerNewUser(String registrationId, String email, String name, String providerId, String imageUrl) {
        User user = User.builder()
            .name(name)
            .email(email.toLowerCase())
            .authProvider(AuthProvider.valueOf(registrationId.toUpperCase()))
            .oauthProviderId(providerId)
            .profileImageUrl(imageUrl)
            .enabled(true)
            .isDemoAccount(false)
            .role(User.Role.USER)
            .build();

        return userRepository.save(user);
    }

    private void updateExistingUser(User existingUser, String name, String imageUrl) {
        existingUser.setName(name);
        existingUser.setProfileImageUrl(imageUrl);
        userRepository.save(existingUser);
    }
}
