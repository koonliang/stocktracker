# Login_03: User Registration with Email and Google OAuth 2.0

## Goal
Enhance the login flow to allow new users to sign up via:
1. **Email registration** - User ID = email address, with password complexity requirements
2. **Google OAuth 2.0** - Sign in/up with Google using OpenID Connect (OIDC)

---

## Password Complexity Requirements
- Minimum 8 characters
- At least 1 uppercase letter (A-Z)
- At least 1 lowercase letter (a-z)
- At least 1 number (0-9)
- At least 1 symbol (!@#$%^&*()_+-=[]{}|;':\",./<>?)

**Regex Pattern:**
```regex
^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*()_+\-=\[\]{}|;':",./<>?]).{8,}$
```

---

## Architecture Overview

```
┌─────────────────┐         ┌─────────────────┐         ┌─────────────────┐
│    Frontend     │         │     Backend     │         │  Google OAuth   │
│   (React SPA)   │         │  (Spring Boot)  │         │     Server      │
└────────┬────────┘         └────────┬────────┘         └────────┬────────┘
         │                           │                           │
         │  POST /api/auth/register  │                           │
         │──────────────────────────>│                           │
         │                           │                           │
         │  GET /oauth2/authorize/   │                           │
         │      google               │                           │
         │──────────────────────────>│                           │
         │                           │  Redirect to Google       │
         │<────────────────────────────────────────────────────>│
         │                           │                           │
         │  Callback with auth code  │                           │
         │──────────────────────────>│  Exchange code for token  │
         │                           │──────────────────────────>│
         │                           │  ID Token + User Info     │
         │                           │<──────────────────────────│
         │  JWT + Redirect           │                           │
         │<──────────────────────────│                           │
```

---

## Implementation Steps

### Phase 1: Backend - Email Registration

#### Step 1.1: Create SignupRequest DTO
**New file:** `backend/src/main/java/com/stocktracker/dto/request/SignupRequest.java`

```java
package com.stocktracker.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SignupRequest {
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{}|;':\",./<>?]).{8,}$",
        message = "Password must contain at least one uppercase letter, one lowercase letter, one number, and one symbol"
    )
    private String password;

    @NotBlank(message = "Password confirmation is required")
    private String confirmPassword;
}
```

#### Step 1.2: Create PasswordValidator Utility
**New file:** `backend/src/main/java/com/stocktracker/util/PasswordValidator.java`

```java
package com.stocktracker.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class PasswordValidator {
    private static final int MIN_LENGTH = 8;
    private static final Pattern UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE = Pattern.compile("[a-z]");
    private static final Pattern DIGIT = Pattern.compile("\\d");
    private static final Pattern SYMBOL = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{}|;':\",./<>?]");

    public static List<String> validate(String password) {
        List<String> errors = new ArrayList<>();
        
        if (password == null || password.length() < MIN_LENGTH) {
            errors.add("Password must be at least " + MIN_LENGTH + " characters");
        }
        if (password != null) {
            if (!UPPERCASE.matcher(password).find()) {
                errors.add("Password must contain at least one uppercase letter");
            }
            if (!LOWERCASE.matcher(password).find()) {
                errors.add("Password must contain at least one lowercase letter");
            }
            if (!DIGIT.matcher(password).find()) {
                errors.add("Password must contain at least one number");
            }
            if (!SYMBOL.matcher(password).find()) {
                errors.add("Password must contain at least one symbol (!@#$%^&*()_+-=[]{}|;':\",./<>?)");
            }
        }
        return errors;
    }

    public static boolean isValid(String password) {
        return validate(password).isEmpty();
    }
}
```

#### Step 1.3: Create UserService Registration Method
**Modify file:** `backend/src/main/java/com/stocktracker/service/UserService.java`

Add registration logic:
```java
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
        .authProvider(AuthProvider.LOCAL)
        .build();
    
    return userRepository.save(user);
}
```

#### Step 1.4: Update User Entity for OAuth Support
**Modify file:** `backend/src/main/java/com/stocktracker/entity/User.java`

Add OAuth fields:
```java
// Add new enum for auth provider
public enum AuthProvider {
    LOCAL,    // Email/password registration
    GOOGLE    // Google OAuth
}

// Add new fields
@Enumerated(EnumType.STRING)
@Column(name = "auth_provider", nullable = false)
@Builder.Default
private AuthProvider authProvider = AuthProvider.LOCAL;

@Column(name = "oauth_provider_id")
private String oauthProviderId;  // Google's unique user ID

@Column(name = "profile_image_url")
private String profileImageUrl;

// Modify password field to allow null for OAuth users
@Size(max = 255)
@Column(nullable = true)  // Changed from @NotBlank
private String password;
```

#### Step 1.5: Add Registration Endpoint to AuthController
**Modify file:** `backend/src/main/java/com/stocktracker/controller/AuthController.java`

```java
@PostMapping("/register")
public ResponseEntity<ApiResponse<AuthResponse>> register(
        @Valid @RequestBody SignupRequest request) {
    User user = userService.registerUser(request);
    
    // Auto-login after registration
    UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
    String token = jwtTokenProvider.generateToken(userDetails);
    
    AuthResponse response = AuthResponse.builder()
        .token(token)
        .type("Bearer")
        .userId(user.getId())
        .email(user.getEmail())
        .name(user.getName())
        .build();
    
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Registration successful", response));
}
```

#### Step 1.6: Update UserRepository
**Modify file:** `backend/src/main/java/com/stocktracker/repository/UserRepository.java`

```java
// Add method for OAuth lookup
Optional<User> findByOauthProviderIdAndAuthProvider(String oauthProviderId, User.AuthProvider authProvider);
```

---

### Phase 2: Backend - Google OAuth 2.0

#### Step 2.1: Add OAuth2 Dependencies
**Modify file:** `backend/pom.xml`

Add to dependencies:
```xml
<!-- OAuth2 Client -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-client</artifactId>
</dependency>

<!-- OAuth2 Resource Server (for JWT validation if needed) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
```

#### Step 2.2: Configure OAuth2 Properties
**Modify file:** `backend/src/main/resources/application.yml`

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}
            scope:
              - email
              - profile
              - openid
            redirect-uri: "{baseUrl}/api/auth/oauth2/callback/{registrationId}"
        provider:
          google:
            authorization-uri: https://accounts.google.com/o/oauth2/v2/auth
            token-uri: https://oauth2.googleapis.com/token
            user-info-uri: https://openidconnect.googleapis.com/v1/userinfo
            jwk-set-uri: https://www.googleapis.com/oauth2/v3/certs
            user-name-attribute: sub
```

**Modify file:** `backend/src/main/resources/application-dev.yml`
```yaml
# For local development, add placeholder values
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID:your-dev-client-id}
            client-secret: ${GOOGLE_CLIENT_SECRET:your-dev-client-secret}
```

#### Step 2.3: Create OAuth2UserInfo Class
**New file:** `backend/src/main/java/com/stocktracker/security/oauth2/OAuth2UserInfo.java`

```java
package com.stocktracker.security.oauth2;

import java.util.Map;

public abstract class OAuth2UserInfo {
    protected Map<String, Object> attributes;

    public OAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public abstract String getId();
    public abstract String getName();
    public abstract String getEmail();
    public abstract String getImageUrl();
}
```

#### Step 2.4: Create GoogleOAuth2UserInfo
**New file:** `backend/src/main/java/com/stocktracker/security/oauth2/GoogleOAuth2UserInfo.java`

```java
package com.stocktracker.security.oauth2;

import java.util.Map;

public class GoogleOAuth2UserInfo extends OAuth2UserInfo {

    public GoogleOAuth2UserInfo(Map<String, Object> attributes) {
        super(attributes);
    }

    @Override
    public String getId() {
        return (String) attributes.get("sub");
    }

    @Override
    public String getName() {
        return (String) attributes.get("name");
    }

    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }

    @Override
    public String getImageUrl() {
        return (String) attributes.get("picture");
    }
}
```

#### Step 2.5: Create OAuth2UserInfoFactory
**New file:** `backend/src/main/java/com/stocktracker/security/oauth2/OAuth2UserInfoFactory.java`

```java
package com.stocktracker.security.oauth2;

import com.stocktracker.entity.User.AuthProvider;
import com.stocktracker.exception.BadRequestException;

import java.util.Map;

public class OAuth2UserInfoFactory {
    
    public static OAuth2UserInfo getOAuth2UserInfo(String registrationId, Map<String, Object> attributes) {
        if (registrationId.equalsIgnoreCase(AuthProvider.GOOGLE.name())) {
            return new GoogleOAuth2UserInfo(attributes);
        } else {
            throw new BadRequestException("Login with " + registrationId + " is not supported");
        }
    }
}
```

#### Step 2.6: Create CustomOAuth2UserService
**New file:** `backend/src/main/java/com/stocktracker/security/oauth2/CustomOAuth2UserService.java`

```java
package com.stocktracker.security.oauth2;

import com.stocktracker.entity.User;
import com.stocktracker.entity.User.AuthProvider;
import com.stocktracker.exception.BadRequestException;
import com.stocktracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);
        return processOAuth2User(userRequest, oauth2User);
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oauth2User) {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(registrationId, oauth2User.getAttributes());

        if (userInfo.getEmail() == null || userInfo.getEmail().isEmpty()) {
            throw new BadRequestException("Email not found from OAuth2 provider");
        }

        Optional<User> existingUser = userRepository.findByEmail(userInfo.getEmail());

        User user;
        if (existingUser.isPresent()) {
            user = existingUser.get();
            // Check if user registered with different provider
            if (!user.getAuthProvider().name().equalsIgnoreCase(registrationId)) {
                throw new BadRequestException(
                    "You're already signed up with " + user.getAuthProvider() + 
                    ". Please use your " + user.getAuthProvider() + " account to login."
                );
            }
            // Update existing user info
            user = updateExistingUser(user, userInfo);
        } else {
            // Register new user
            user = registerNewUser(registrationId, userInfo);
        }

        return new CustomOAuth2User(user, oauth2User.getAttributes());
    }

    private User registerNewUser(String registrationId, OAuth2UserInfo userInfo) {
        User user = User.builder()
            .name(userInfo.getName())
            .email(userInfo.getEmail().toLowerCase())
            .authProvider(AuthProvider.valueOf(registrationId.toUpperCase()))
            .oauthProviderId(userInfo.getId())
            .profileImageUrl(userInfo.getImageUrl())
            .enabled(true)
            .isDemoAccount(false)
            .role(User.Role.USER)
            .build();

        return userRepository.save(user);
    }

    private User updateExistingUser(User existingUser, OAuth2UserInfo userInfo) {
        existingUser.setName(userInfo.getName());
        existingUser.setProfileImageUrl(userInfo.getImageUrl());
        return userRepository.save(existingUser);
    }
}
```

#### Step 2.7: Create CustomOAuth2User
**New file:** `backend/src/main/java/com/stocktracker/security/oauth2/CustomOAuth2User.java`

```java
package com.stocktracker.security.oauth2;

import com.stocktracker.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

@Getter
public class CustomOAuth2User implements OAuth2User {
    private final User user;
    private final Map<String, Object> attributes;

    public CustomOAuth2User(User user, Map<String, Object> attributes) {
        this.user = user;
        this.attributes = attributes;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public String getName() {
        return user.getEmail();
    }
}
```

#### Step 2.8: Create OAuth2AuthenticationSuccessHandler
**New file:** `backend/src/main/java/com/stocktracker/security/oauth2/OAuth2AuthenticationSuccessHandler.java`

```java
package com.stocktracker.security.oauth2;

import com.stocktracker.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;

    @Value("${app.oauth2.redirectUri:http://localhost:3000/oauth2/redirect}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        if (response.isCommitted()) {
            return;
        }

        CustomOAuth2User oauth2User = (CustomOAuth2User) authentication.getPrincipal();
        UserDetails userDetails = userDetailsService.loadUserByUsername(oauth2User.getUser().getEmail());
        String token = jwtTokenProvider.generateToken(userDetails);

        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
            .queryParam("token", token)
            .queryParam("userId", oauth2User.getUser().getId())
            .queryParam("email", oauth2User.getUser().getEmail())
            .queryParam("name", oauth2User.getUser().getName())
            .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
```

#### Step 2.9: Create OAuth2AuthenticationFailureHandler
**New file:** `backend/src/main/java/com/stocktracker/security/oauth2/OAuth2AuthenticationFailureHandler.java`

```java
package com.stocktracker.security.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Value("${app.oauth2.redirectUri:http://localhost:3000/oauth2/redirect}")
    private String redirectUri;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
            .queryParam("error", URLEncoder.encode(exception.getLocalizedMessage(), StandardCharsets.UTF_8))
            .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
```

#### Step 2.10: Update SecurityConfig for OAuth2
**Modify file:** `backend/src/main/java/com/stocktracker/config/SecurityConfig.java`

```java
// Add new injections
private final CustomOAuth2UserService customOAuth2UserService;
private final OAuth2AuthenticationSuccessHandler oAuth2SuccessHandler;
private final OAuth2AuthenticationFailureHandler oAuth2FailureHandler;

// Update securityFilterChain method
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(auth -> auth
            // ... existing permitAll rules ...
            .requestMatchers(
                "/api/auth/**",
                "/oauth2/**",           // Add OAuth2 endpoints
                "/login/oauth2/**",     // Add OAuth2 login endpoints
                // ... other rules
            ).permitAll()
            .anyRequest().authenticated()
        )
        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        )
        // Add OAuth2 Login configuration
        .oauth2Login(oauth2 -> oauth2
            .authorizationEndpoint(authorization -> authorization
                .baseUri("/oauth2/authorize")
            )
            .redirectionEndpoint(redirection -> redirection
                .baseUri("/api/auth/oauth2/callback/*")
            )
            .userInfoEndpoint(userInfo -> userInfo
                .userService(customOAuth2UserService)
            )
            .successHandler(oAuth2SuccessHandler)
            .failureHandler(oAuth2FailureHandler)
        )
        .authenticationProvider(authenticationProvider())
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
        .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

    return http.build();
}
```

#### Step 2.11: Add OAuth2 Redirect URI Configuration
**Modify file:** `backend/src/main/resources/application.yml`

```yaml
# App-specific configuration
app:
  oauth2:
    redirectUri: ${OAUTH2_REDIRECT_URI:http://localhost:3000/oauth2/redirect}
```

---

### Phase 3: Frontend - Registration Form

#### Step 3.1: Create Registration Types
**Modify file:** `frontend/src/types/auth.ts`

```typescript
export interface SignupRequest {
  name: string
  email: string
  password: string
  confirmPassword: string
}

export interface PasswordValidation {
  minLength: boolean
  hasUppercase: boolean
  hasLowercase: boolean
  hasNumber: boolean
  hasSymbol: boolean
  isValid: boolean
}
```

#### Step 3.2: Create Password Validation Hook
**New file:** `frontend/src/hooks/usePasswordValidation.ts`

```typescript
import { useMemo } from 'react'
import type { PasswordValidation } from '@/types/auth'

const MIN_LENGTH = 8
const UPPERCASE_REGEX = /[A-Z]/
const LOWERCASE_REGEX = /[a-z]/
const NUMBER_REGEX = /\d/
const SYMBOL_REGEX = /[!@#$%^&*()_+\-=\[\]{}|;':",./<>?]/

export function usePasswordValidation(password: string): PasswordValidation {
  return useMemo(() => {
    const minLength = password.length >= MIN_LENGTH
    const hasUppercase = UPPERCASE_REGEX.test(password)
    const hasLowercase = LOWERCASE_REGEX.test(password)
    const hasNumber = NUMBER_REGEX.test(password)
    const hasSymbol = SYMBOL_REGEX.test(password)
    const isValid = minLength && hasUppercase && hasLowercase && hasNumber && hasSymbol

    return {
      minLength,
      hasUppercase,
      hasLowercase,
      hasNumber,
      hasSymbol,
      isValid,
    }
  }, [password])
}
```

#### Step 3.3: Create PasswordStrengthIndicator Component
**New file:** `frontend/src/components/auth/PasswordStrengthIndicator.tsx`

```typescript
import type { PasswordValidation } from '@/types/auth'
import styles from './PasswordStrengthIndicator.module.css'

interface Props {
  validation: PasswordValidation
  show: boolean
}

const requirements = [
  { key: 'minLength', label: 'At least 8 characters' },
  { key: 'hasUppercase', label: 'One uppercase letter' },
  { key: 'hasLowercase', label: 'One lowercase letter' },
  { key: 'hasNumber', label: 'One number' },
  { key: 'hasSymbol', label: 'One symbol (!@#$%^&*...)' },
] as const

export function PasswordStrengthIndicator({ validation, show }: Props) {
  if (!show) return null

  return (
    <div className={styles.container}>
      <p className={styles.title}>Password requirements:</p>
      <ul className={styles.list}>
        {requirements.map(({ key, label }) => (
          <li
            key={key}
            className={`${styles.item} ${validation[key] ? styles.valid : styles.invalid}`}
          >
            <span className={styles.icon}>{validation[key] ? '✓' : '○'}</span>
            {label}
          </li>
        ))}
      </ul>
    </div>
  )
}
```

#### Step 3.4: Create PasswordStrengthIndicator Styles
**New file:** `frontend/src/components/auth/PasswordStrengthIndicator.module.css`

```css
.container {
  margin-top: 0.5rem;
  padding: 0.75rem;
  background: #F8FAFC;
  border-radius: 6px;
  border: 1px solid #E2E8F0;
}

.title {
  font-size: 0.75rem;
  font-weight: 600;
  color: #64748B;
  margin: 0 0 0.5rem;
}

.list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.item {
  font-size: 0.75rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  transition: color 0.2s;
}

.icon {
  width: 1rem;
  text-align: center;
}

.valid {
  color: #10B981;
}

.invalid {
  color: #94A3B8;
}
```

#### Step 3.5: Add Registration Method to AuthService
**Modify file:** `frontend/src/services/authService.ts`

```typescript
async register(data: SignupRequest): Promise<AuthResponse> {
  try {
    const response = await api.post<ApiResponse<AuthResponse>>('/auth/register', data)

    if (response.data.success && response.data.data) {
      const authData = response.data.data
      localStorage.setItem(TOKEN_KEY, authData.token)
      localStorage.setItem(
        USER_KEY,
        JSON.stringify({
          id: authData.userId,
          email: authData.email,
          name: authData.name,
        })
      )
      return authData
    }

    throw new Error(response.data.message || 'Registration failed')
  } catch (error: unknown) {
    const message =
      (error as { response?: { data?: { message?: string } }; message?: string }).response?.data
        ?.message ||
      (error as { message?: string }).message ||
      'Registration failed'
    throw new Error(message)
  }
}
```

#### Step 3.6: Create Registration Page
**New file:** `frontend/src/pages/Register/Register.tsx`

```typescript
import { useState, type FormEvent } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { authService } from '@services/authService'
import { usePasswordValidation } from '@hooks/usePasswordValidation'
import { PasswordStrengthIndicator } from '@components/auth/PasswordStrengthIndicator'
import styles from './Register.module.css'

const Register = () => {
  const navigate = useNavigate()
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [error, setError] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const [showPasswordRequirements, setShowPasswordRequirements] = useState(false)

  const passwordValidation = usePasswordValidation(password)

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setError('')

    // Validation
    if (!name.trim()) {
      setError('Please enter your name')
      return
    }
    if (!email || !email.includes('@')) {
      setError('Please enter a valid email address')
      return
    }
    if (!passwordValidation.isValid) {
      setError('Please meet all password requirements')
      return
    }
    if (password !== confirmPassword) {
      setError('Passwords do not match')
      return
    }

    setIsLoading(true)

    try {
      await authService.register({ name, email, password, confirmPassword })
      navigate('/dashboard', { replace: true })
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Registration failed')
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <div className={styles.container}>
      <div className={styles.card}>
        <h1 className={styles.title}>Create Account</h1>
        <p className={styles.subtitle}>Join Stock Tracker today</p>

        <form onSubmit={handleSubmit} className={styles.form}>
          {error && <div className={styles.error}>{error}</div>}

          {/* Name field */}
          <div className={styles.field}>
            <label htmlFor="name" className={styles.label}>Name</label>
            <input
              id="name"
              type="text"
              value={name}
              onChange={e => setName(e.target.value)}
              className={styles.input}
              placeholder="Enter your name"
              disabled={isLoading}
              autoComplete="name"
            />
          </div>

          {/* Email field */}
          <div className={styles.field}>
            <label htmlFor="email" className={styles.label}>Email</label>
            <input
              id="email"
              type="email"
              value={email}
              onChange={e => setEmail(e.target.value)}
              className={styles.input}
              placeholder="Enter your email"
              disabled={isLoading}
              autoComplete="email"
            />
          </div>

          {/* Password field */}
          <div className={styles.field}>
            <label htmlFor="password" className={styles.label}>Password</label>
            <input
              id="password"
              type="password"
              value={password}
              onChange={e => setPassword(e.target.value)}
              onFocus={() => setShowPasswordRequirements(true)}
              className={styles.input}
              placeholder="Create a password"
              disabled={isLoading}
              autoComplete="new-password"
            />
            <PasswordStrengthIndicator
              validation={passwordValidation}
              show={showPasswordRequirements && password.length > 0}
            />
          </div>

          {/* Confirm Password field */}
          <div className={styles.field}>
            <label htmlFor="confirmPassword" className={styles.label}>Confirm Password</label>
            <input
              id="confirmPassword"
              type="password"
              value={confirmPassword}
              onChange={e => setConfirmPassword(e.target.value)}
              className={styles.input}
              placeholder="Confirm your password"
              disabled={isLoading}
              autoComplete="new-password"
            />
          </div>

          <button type="submit" className={styles.button} disabled={isLoading}>
            {isLoading ? 'Creating Account...' : 'Create Account'}
          </button>
        </form>

        {/* Divider */}
        <div className={styles.divider}>
          <span>or</span>
        </div>

        {/* Google OAuth Button */}
        <button
          type="button"
          className={styles.googleButton}
          onClick={() => window.location.href = '/oauth2/authorize/google'}
          disabled={isLoading}
        >
          <GoogleIcon />
          Continue with Google
        </button>

        {/* Link to login */}
        <p className={styles.footer}>
          Already have an account? <Link to="/login" className={styles.link}>Sign in</Link>
        </p>
      </div>
    </div>
  )
}

// Google Icon SVG component
const GoogleIcon = () => (
  <svg className={styles.googleIcon} viewBox="0 0 24 24">
    <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
    <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
    <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
    <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
  </svg>
)

export default Register
```

#### Step 3.7: Create Register Page Styles
**New file:** `frontend/src/pages/Register/Register.module.css`

Copy base styles from Login.module.css and add:
```css
/* Additional styles for registration page */

.divider {
  display: flex;
  align-items: center;
  margin: 1.5rem 0;
}

.divider::before,
.divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: #E2E8F0;
}

.divider span {
  padding: 0 1rem;
  font-size: 0.875rem;
  color: #94A3B8;
}

.googleButton {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  padding: 0.75rem 1rem;
  font-size: 1rem;
  font-weight: 500;
  font-family: inherit;
  color: #334155;
  background: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.2s, border-color 0.2s;
}

.googleButton:hover:not(:disabled) {
  background: #F8FAFC;
  border-color: #CBD5E1;
}

.googleButton:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.googleIcon {
  width: 1.25rem;
  height: 1.25rem;
}

.footer {
  text-align: center;
  margin-top: 1.5rem;
  font-size: 0.875rem;
  color: #64748B;
}

.link {
  color: #4F46E5;
  text-decoration: none;
  font-weight: 500;
}

.link:hover {
  text-decoration: underline;
}
```

#### Step 3.8: Create OAuth2 Redirect Handler Page
**New file:** `frontend/src/pages/OAuth2Redirect/OAuth2Redirect.tsx`

```typescript
import { useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { authService } from '@services/authService'
import styles from './OAuth2Redirect.module.css'

const OAuth2Redirect = () => {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const token = searchParams.get('token')
    const errorParam = searchParams.get('error')
    const userId = searchParams.get('userId')
    const email = searchParams.get('email')
    const name = searchParams.get('name')

    if (errorParam) {
      setError(decodeURIComponent(errorParam))
      return
    }

    if (token && userId && email && name) {
      // Store auth data from OAuth callback
      authService.storeOAuthCredentials({
        token,
        userId: parseInt(userId, 10),
        email,
        name,
      })
      navigate('/dashboard', { replace: true })
    } else {
      setError('Invalid OAuth response')
    }
  }, [searchParams, navigate])

  if (error) {
    return (
      <div className={styles.container}>
        <div className={styles.card}>
          <h1 className={styles.title}>Authentication Failed</h1>
          <p className={styles.error}>{error}</p>
          <button
            className={styles.button}
            onClick={() => navigate('/login')}
          >
            Back to Login
          </button>
        </div>
      </div>
    )
  }

  return (
    <div className={styles.container}>
      <div className={styles.card}>
        <div className={styles.spinner} />
        <p className={styles.text}>Completing sign in...</p>
      </div>
    </div>
  )
}

export default OAuth2Redirect
```

#### Step 3.9: Add storeOAuthCredentials to AuthService
**Modify file:** `frontend/src/services/authService.ts`

```typescript
storeOAuthCredentials(data: { token: string; userId: number; email: string; name: string }): void {
  localStorage.setItem(TOKEN_KEY, data.token)
  localStorage.setItem(
    USER_KEY,
    JSON.stringify({
      id: data.userId,
      email: data.email,
      name: data.name,
    })
  )
}
```

#### Step 3.10: Update Login Page with Links
**Modify file:** `frontend/src/pages/Login/Login.tsx`

Add below the form:
```typescript
{/* Divider */}
<div className={styles.divider}>
  <span>or</span>
</div>

{/* Google OAuth Button */}
<button
  type="button"
  className={styles.googleButton}
  onClick={() => window.location.href = '/oauth2/authorize/google'}
  disabled={isLoading}
>
  <GoogleIcon />
  Continue with Google
</button>

{/* Link to registration */}
<p className={styles.footer}>
  Don't have an account? <Link to="/register" className={styles.link}>Sign up</Link>
</p>
```

#### Step 3.11: Update Routes
**Modify file:** `frontend/src/App.tsx`

```typescript
import Register from '@pages/Register'
import OAuth2Redirect from '@pages/OAuth2Redirect'

// Add routes
<Route path="/register" element={<Register />} />
<Route path="/oauth2/redirect" element={<OAuth2Redirect />} />
```

---

## Database Migration

If using a persistent database, create migration for new User columns:

```sql
-- Add new columns to users table
ALTER TABLE users ADD COLUMN auth_provider VARCHAR(20) NOT NULL DEFAULT 'LOCAL';
ALTER TABLE users ADD COLUMN oauth_provider_id VARCHAR(255);
ALTER TABLE users ADD COLUMN profile_image_url VARCHAR(500);

-- Allow null password for OAuth users
ALTER TABLE users MODIFY password VARCHAR(255) NULL;

-- Add index for OAuth lookup
CREATE INDEX idx_users_oauth_provider ON users(oauth_provider_id, auth_provider);
```

---

## Environment Variables

### Backend (.env or application-prod.yml)
```properties
GOOGLE_CLIENT_ID=your-google-client-id.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=your-google-client-secret
OAUTH2_REDIRECT_URI=https://your-domain.com/oauth2/redirect
```

### Frontend (.env)
```properties
# No additional env vars needed - OAuth URLs are backend-relative
```

---

## Google Cloud Console Setup

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select existing
3. Enable **Google+ API** and **Google Identity** services
4. Go to **Credentials** → **Create Credentials** → **OAuth 2.0 Client IDs**
5. Configure consent screen with required scopes: `email`, `profile`, `openid`
6. Add authorized redirect URIs:
   - Development: `http://localhost:8080/api/auth/oauth2/callback/google`
   - Production: `https://your-domain.com/api/auth/oauth2/callback/google`
7. Copy Client ID and Client Secret to environment variables

---

## Files Summary

| Action | File | Description |
|--------|------|-------------|
| Create | `backend/.../dto/request/SignupRequest.java` | Registration request DTO with validation |
| Create | `backend/.../util/PasswordValidator.java` | Server-side password validation utility |
| Modify | `backend/.../entity/User.java` | Add AuthProvider enum and OAuth fields |
| Modify | `backend/.../service/UserService.java` | Add registerUser method |
| Modify | `backend/.../controller/AuthController.java` | Add /register endpoint |
| Modify | `backend/.../repository/UserRepository.java` | Add OAuth lookup method |
| Modify | `backend/pom.xml` | Add OAuth2 dependencies |
| Modify | `backend/.../resources/application.yml` | Add OAuth2 configuration |
| Modify | `backend/.../config/SecurityConfig.java` | Configure OAuth2 login |
| Create | `backend/.../security/oauth2/OAuth2UserInfo.java` | Abstract OAuth user info |
| Create | `backend/.../security/oauth2/GoogleOAuth2UserInfo.java` | Google-specific user info |
| Create | `backend/.../security/oauth2/OAuth2UserInfoFactory.java` | Factory for provider-specific info |
| Create | `backend/.../security/oauth2/CustomOAuth2UserService.java` | OAuth user processing service |
| Create | `backend/.../security/oauth2/CustomOAuth2User.java` | Custom OAuth principal |
| Create | `backend/.../security/oauth2/OAuth2AuthenticationSuccessHandler.java` | JWT generation on OAuth success |
| Create | `backend/.../security/oauth2/OAuth2AuthenticationFailureHandler.java` | OAuth failure handling |
| Modify | `frontend/src/types/auth.ts` | Add SignupRequest and PasswordValidation types |
| Create | `frontend/src/hooks/usePasswordValidation.ts` | Password validation hook |
| Create | `frontend/src/components/auth/PasswordStrengthIndicator.tsx` | Password requirements UI |
| Create | `frontend/src/components/auth/PasswordStrengthIndicator.module.css` | Indicator styles |
| Modify | `frontend/src/services/authService.ts` | Add register and storeOAuthCredentials methods |
| Create | `frontend/src/pages/Register/Register.tsx` | Registration page component |
| Create | `frontend/src/pages/Register/Register.module.css` | Registration page styles |
| Create | `frontend/src/pages/Register/index.ts` | Barrel export |
| Create | `frontend/src/pages/OAuth2Redirect/OAuth2Redirect.tsx` | OAuth callback handler |
| Create | `frontend/src/pages/OAuth2Redirect/OAuth2Redirect.module.css` | OAuth redirect styles |
| Create | `frontend/src/pages/OAuth2Redirect/index.ts` | Barrel export |
| Modify | `frontend/src/pages/Login/Login.tsx` | Add Google button and register link |
| Modify | `frontend/src/pages/Login/Login.module.css` | Add divider and Google button styles |
| Modify | `frontend/src/App.tsx` | Add /register and /oauth2/redirect routes |

---

## Testing Checklist

### Email Registration
- [ ] Form validation shows errors for empty fields
- [ ] Email format validation works
- [ ] Password strength indicator shows/hides on focus
- [ ] All password requirements display with check/unchecked states
- [ ] Password confirmation mismatch shows error
- [ ] Duplicate email shows appropriate error
- [ ] Successful registration redirects to dashboard
- [ ] JWT token stored in localStorage

### Google OAuth
- [ ] "Continue with Google" redirects to Google consent screen
- [ ] Successful Google auth creates new user and redirects to dashboard
- [ ] Existing email with different provider shows error message
- [ ] Existing Google user updates profile info on re-login
- [ ] OAuth failure shows error on redirect page
- [ ] JWT token stored after OAuth success

### Security
- [ ] Password never logged or exposed in responses
- [ ] BCrypt encoding applied to passwords
- [ ] OAuth state parameter prevents CSRF
- [ ] Redirect URI validation prevents open redirects

---

## Future Considerations

1. **Email Verification** - Send confirmation email before enabling account
2. **Password Reset** - Forgot password flow with email token
3. **Remember Me** - Extended session duration option
4. **MFA/2FA** - Add TOTP or SMS verification
5. **Social Providers** - Add GitHub, Microsoft, Apple sign-in options
6. **Rate Limiting** - Prevent brute force on registration endpoint
