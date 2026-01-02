package com.stocktracker.repository;

import com.stocktracker.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByIsDemoAccountTrueAndCreatedAtBefore(LocalDateTime cutoffTime);
    Optional<User> findByOauthProviderIdAndAuthProvider(String oauthProviderId, User.AuthProvider authProvider);
}
