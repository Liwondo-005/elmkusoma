package tz.elmkusoma.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.InstitutionMembershipRepository;
import tz.elmkusoma.shared.repository.UserRepository;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pins the P1 bootstrap contract: opt-in, create-only. Bootstrap must never
 * reset credentials of an existing privileged account (restart must not
 * silently restore a default password).
 */
@ExtendWith(MockitoExtension.class)
class AdminUserInitializerTest {

    @Mock private UserRepository userRepository;
    @Mock private InstitutionMembershipRepository membershipRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private ApplicationArguments args;

    @InjectMocks
    private AdminUserInitializer initializer;

    private static final String ADMIN_EMAIL = "admin@elmkusoma.go.tz";

    private static void setField(AdminUserInitializer target, String name, Object value) throws Exception {
        var field = AdminUserInitializer.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static void configure(AdminUserInitializer target, boolean enabled, String password) throws Exception {
        setField(target, "bootstrapEnabled", enabled);
        setField(target, "bootstrapPassword", password);
    }

    private static User healthyAdmin(String hash) {
        return User.builder()
                .id(UUID.randomUUID())
                .email(ADMIN_EMAIL)
                .passwordHash(hash)
                .firstName("Platform")
                .lastName("Admin")
                .role(User.Role.ADMIN)
                .isActive(true)
                .isEmailVerified(true)
                .build();
    }

    @Test
    void bootstrapDisabled_run_isCompleteNoOp() throws Exception {
        configure(initializer, false, "any-secret");

        initializer.run(args);

        verifyNoInteractions(userRepository, membershipRepository, passwordEncoder);
    }

    @Test
    void enabledButPasswordMissing_failsFast() throws Exception {
        configure(initializer, true, "");

        assertThrows(IllegalStateException.class, () -> initializer.run(args));
        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test
    void existingAdmin_envSet_passwordUntouched_neverRotated() throws Exception {
        configure(initializer, true, "rotated-secret");
        User admin = healthyAdmin("existing-hash");
        when(userRepository.findByEmailAndIsDeletedFalse(ADMIN_EMAIL))
                .thenReturn(Optional.of(admin));
        when(membershipRepository.existsByUserIdAndInstitutionIdAndIsActiveTrue(any(), any()))
                .thenReturn(true);

        initializer.run(args);

        assertEquals("existing-hash", admin.getPasswordHash());
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void missingAdmin_envSet_createdWithEnvValue() throws Exception {
        configure(initializer, true, "env-secret");
        UUID adminId = UUID.randomUUID();
        User persisted = healthyAdmin("env-hash");
        persisted.setId(adminId);
        when(userRepository.findByEmailAndIsDeletedFalse(ADMIN_EMAIL))
                .thenReturn(Optional.empty(), Optional.of(persisted));
        when(passwordEncoder.encode("env-secret")).thenReturn("env-hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(membershipRepository.existsByUserIdAndInstitutionIdAndIsActiveTrue(any(), any()))
                .thenReturn(true);

        initializer.run(args);

        verify(passwordEncoder).encode("env-secret");
        verify(passwordEncoder, never()).encode("password");
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("env-hash", captor.getValue().getPasswordHash());
        assertEquals(ADMIN_EMAIL, captor.getValue().getEmail());
    }
}
