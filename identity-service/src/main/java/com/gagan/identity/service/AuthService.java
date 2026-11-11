package com.gagan.identity.service;

import com.gagan.identity.constant.ErrorCodeEnum;
import com.gagan.identity.dto.AuthResponse;
import com.gagan.identity.dto.LoginRequest;
import com.gagan.identity.dto.RegisterRequest;
import com.gagan.identity.dto.UserResponse;
import com.gagan.identity.entity.Role;
import com.gagan.identity.entity.User;
import com.gagan.identity.exception.IdentityServiceException;
import com.gagan.identity.repository.UserRepository;
import com.gagan.identity.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public UserResponse registerUser(RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IdentityServiceException(ErrorCodeEnum.EMAIL_ALREADY_EXISTS);
        }

        User user = User.builder()
                .fullName(request.getFullName().trim())
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.CUSTOMER)
                .build();

        User savedUser = userRepository.save(user);
        log.info("Registered new CUSTOMER account with userId [{}] and email [{}]",
                savedUser.getUserId(), savedUser.getEmail());

        return UserResponse.builder()
                .userId(savedUser.getUserId())
                .fullName(savedUser.getFullName())
                .email(savedUser.getEmail())
                .role(savedUser.getRole().name())
                .createdAt(savedUser.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> {
                    log.warn("Login failed: unknown email [{}]", normalizedEmail);
                    return new IdentityServiceException(ErrorCodeEnum.INVALID_CREDENTIALS);
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Login failed: bad password for email [{}]", normalizedEmail);
            throw new IdentityServiceException(ErrorCodeEnum.INVALID_CREDENTIALS);
        }

        String token = jwtService.generateToken(user);
        log.info("Authenticated user [{}] with role [{}]", user.getEmail(), user.getRole());

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationSeconds())
                .userId(user.getUserId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }
}