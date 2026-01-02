package com.stocktracker.security;

import com.stocktracker.entity.User;
import com.stocktracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        // For OAuth users, password is null, so use a placeholder
        // This is safe because OAuth users can't login via password
        String password = user.getPassword() != null ? user.getPassword() : "";

        return new org.springframework.security.core.userdetails.User(
            user.getEmail(),
            password,
            user.isEnabled(),
            true,
            true,
            true,
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }
}
