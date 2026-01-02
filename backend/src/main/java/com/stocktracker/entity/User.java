package com.stocktracker.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(name = "users", uniqueConstraints = {
    @UniqueConstraint(columnNames = "email")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false)
    private String name;

    @NotBlank
    @Email
    @Size(max = 100)
    @Column(nullable = false, unique = true)
    private String email;

    @Size(max = 255)
    @Column(nullable = true)  // Allow null for OAuth users
    private String password;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @Column(name = "is_demo_account", nullable = false)
    @Builder.Default
    private boolean isDemoAccount = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Role role = Role.USER;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", nullable = false)
    @Builder.Default
    private AuthProvider authProvider = AuthProvider.LOCAL;

    @Column(name = "oauth_provider_id")
    private String oauthProviderId;  // Google's unique user ID

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    public enum Role {
        USER, ADMIN
    }

    public enum AuthProvider {
        LOCAL,    // Email/password registration
        GOOGLE    // Google OAuth
    }
}
