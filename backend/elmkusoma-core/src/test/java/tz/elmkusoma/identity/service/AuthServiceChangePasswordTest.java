package tz.elmkusoma.identity.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import tz.elmkusoma.identity.dto.request.ChangePasswordRequest;
import tz.elmkusoma.identity.service.impl.AuthServiceImpl;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.UserRepository;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Self-service password change: success path re-hashes via BCrypt and a wrong
 * current password surfaces as {@link BadCredentialsException} — the same type
 * login throws, so {@code GlobalExceptionHandler} renders both as an identical
 * 401 {@code "Invalid credentials"} response.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceChangePasswordTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthServiceImpl authService;

    private User existingUser(String passwordHash) {
        return User.builder()
                .id(UUID.randomUUID())
                .email("john@example.com")
                .firstName("John")
                .lastName("Doe")
                .role(User.Role.STUDENT)
                .isActive(true)
                .isEmailVerified(true)
                .passwordHash(passwordHash)
                .build();
    }

    private ChangePasswordRequest request(String currentPassword, String newPassword) {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword(currentPassword);
        request.setNewPassword(newPassword);
        return request;
    }

    @Test
    void changePassword_shouldVerifyCurrentAndSaveNewHash() {
        User user = existingUser("old_hash");
        when(userRepository.findByEmailAndIsDeletedFalse("john@example.com"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldpassword123", "old_hash")).thenReturn(true);
        when(passwordEncoder.encode("newpassword123")).thenReturn("encoded_new_password");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        authService.changePassword("john@example.com", request("oldpassword123", "newpassword123"));

        assertEquals("encoded_new_password", user.getPasswordHash());
        verify(passwordEncoder).matches("oldpassword123", "old_hash");
        verify(userRepository).save(user);
    }

    @Test
    void changePassword_withWrongCurrentPassword_shouldThrowBadCredentials() {
        User user = existingUser("old_hash");
        when(userRepository.findByEmailAndIsDeletedFalse("john@example.com"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpassword", "old_hash")).thenReturn(false);

        assertThrows(BadCredentialsException.class,
                () -> authService.changePassword("john@example.com", request("wrongpassword", "newpassword123")));

        assertEquals("old_hash", user.getPasswordHash());
        verify(userRepository, never()).save(any());
    }
}
