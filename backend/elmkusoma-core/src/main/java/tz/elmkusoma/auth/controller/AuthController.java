package tz.elmkusoma.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.auth.dto.AuthResponse;
import tz.elmkusoma.auth.dto.LoginRequest;
import tz.elmkusoma.auth.dto.RegisterRequest;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.config.JwtTokenProvider;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.domain.Institution;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.shared.repository.InstitutionRepository;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final InstitutionRepository institutionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        if (userRepository.existsByEmailAndIsDeletedFalse(request.getEmail())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("An account with this email already exists"));
        }

        Institution institution = institutionRepository.findByIsDeletedFalse().stream().findFirst()
                .orElseGet(() -> {
                    Institution newInst = Institution.builder()
                            .name("Default Institution")
                            .code("DEFAULT")
                            .type(Institution.InstitutionType.SECONDARY)
                            .country("Tanzania")
                            .isActive(true)
                            .build();
                    return institutionRepository.save(newInst);
                });

        User user = User.builder()
                .institutionId(institution.getId())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .middleName(request.getMiddleName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .role(User.Role.valueOf(request.getRole().toUpperCase()))
                .isActive(true)
                .isEmailVerified(false)
                .build();

        User savedUser = userRepository.save(user);

        String accessToken = jwtTokenProvider.generateToken(savedUser.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(savedUser.getEmail());

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(86400000L)
                .user(AuthResponse.UserDto.builder()
                        .id(savedUser.getId().toString())
                        .email(savedUser.getEmail())
                        .firstName(savedUser.getFirstName())
                        .lastName(savedUser.getLastName())
                        .fullName(savedUser.getFullName())
                        .role(savedUser.getRole().name())
                        .institutionId(savedUser.getInstitutionId().toString())
                        .build())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registration successful", authResponse));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String accessToken = jwtTokenProvider.generateToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(request.getEmail());

        tz.elmkusoma.shared.domain.User user = userRepository.findByEmailAndIsDeletedFalse(request.getEmail())
                .orElseThrow();

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(86400000L)
                .user(AuthResponse.UserDto.builder()
                        .id(user.getId().toString())
                        .email(user.getEmail())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .fullName(user.getFullName())
                        .role(user.getRole().name())
                        .institutionId(user.getInstitutionId().toString())
                        .build())
                .build();

        return ResponseEntity.ok(ApiResponse.success("Login successful", authResponse));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthResponse.UserDto>> getCurrentUser(
            @RequestHeader("X-User-Id") UUID userId) {
        tz.elmkusoma.shared.domain.User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        AuthResponse.UserDto userDto = AuthResponse.UserDto.builder()
                .id(user.getId().toString())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .institutionId(user.getInstitutionId().toString())
                .build();

        return ResponseEntity.ok(ApiResponse.success(userDto));
    }
}
