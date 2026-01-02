package com.stocktracker.service;

import com.stocktracker.dto.request.SignupRequest;
import com.stocktracker.entity.User;
import com.stocktracker.exception.BadRequestException;
import com.stocktracker.exception.ResourceNotFoundException;
import com.stocktracker.repository.UserRepository;
import com.stocktracker.util.PasswordValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User getUserById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    @Transactional
    public User registerUser(SignupRequest request) {
        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email address is already registered");
        }
        
        // Validate passwords match
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }
        
        // Validate password complexity (additional server-side validation)
        List<String> passwordErrors = PasswordValidator.validate(request.getPassword());
        if (!passwordErrors.isEmpty()) {
            throw new BadRequestException(String.join(", ", passwordErrors));
        }
        
        // Create new user
        User user = User.builder()
            .name(request.getName())
            .email(request.getEmail().toLowerCase().trim())
            .password(passwordEncoder.encode(request.getPassword()))
            .enabled(true)
            .isDemoAccount(false)
            .role(User.Role.USER)
            .authProvider(User.AuthProvider.LOCAL)
            .build();
        
        return userRepository.save(user);
    }
}
