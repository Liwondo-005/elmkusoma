package tz.elmkusoma.identity.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import tz.elmkusoma.config.security.JwtTokenProvider;
import tz.elmkusoma.identity.dto.request.RegisterRequest;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.student.repository.StudentClassAssignmentRepository;
import tz.elmkusoma.student.repository.StudentRepository;
import tz.elmkusoma.identity.repository.EmailVerificationTokenRepository;
import tz.elmkusoma.identity.repository.PasswordResetTokenRepository;
import tz.elmkusoma.identity.repository.RevokedTokenRepository;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplRegistrationTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private StudentRepository studentRepository;
    @Mock private StudentClassAssignmentRepository studentClassAssignmentRepository;
    @Mock private EmailVerificationTokenRepository emailVerificationTokenRepository;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private RevokedTokenRepository revokedTokenRepository;

    @InjectMocks
    private AuthServiceImpl authService;

    private User.Role resolveRole(String role) throws Exception {
        Method method = AuthServiceImpl.class.getDeclaredMethod("resolveRegistrationRole", String.class);
        method.setAccessible(true);
        return (User.Role) method.invoke(authService, role);
    }

    @Test
    void nullRoleDefaultsToStudent() throws Exception {
        assertEquals(User.Role.STUDENT, resolveRole(null));
    }

    @Test
    void blankRoleDefaultsToStudent() throws Exception {
        assertEquals(User.Role.STUDENT, resolveRole(""));
        assertEquals(User.Role.STUDENT, resolveRole("  "));
    }

    @Test
    void studentRoleAccepted() throws Exception {
        assertEquals(User.Role.STUDENT, resolveRole("STUDENT"));
    }

    @Test
    void teacherRoleAccepted() throws Exception {
        assertEquals(User.Role.TEACHER, resolveRole("TEACHER"));
    }

    @Test
    void parentRoleAccepted() throws Exception {
        assertEquals(User.Role.PARENT, resolveRole("PARENT"));
    }

    @Test
    void otherLearnerRoleAccepted() throws Exception {
        assertEquals(User.Role.OTHER_LEARNER, resolveRole("OTHER_LEARNER"));
    }

    @Test
    void roleIsCaseInsensitive() throws Exception {
        assertEquals(User.Role.STUDENT, resolveRole("student"));
        assertEquals(User.Role.TEACHER, resolveRole("Teacher"));
        assertEquals(User.Role.PARENT, resolveRole("parent"));
    }

    @Test
    void adminRoleRejected() {
        Exception ex = assertThrows(Exception.class, () -> resolveRole("ADMIN"));
        assertTrue(ex.getCause().getMessage().contains("not allowed for public registration"));
    }

    @Test
    void institutionAdminRoleRejected() {
        Exception ex = assertThrows(Exception.class, () -> resolveRole("INSTITUTION_ADMIN"));
        assertTrue(ex.getCause().getMessage().contains("not allowed for public registration"));
    }

    @Test
    void invalidRoleStringRejected() {
        Exception ex = assertThrows(Exception.class, () -> resolveRole("SUPER_ADMIN"));
        assertTrue(ex.getCause().getMessage().contains("Invalid role"));
    }

    @Test
    void registerDuplicateEmailThrows() {
        when(userRepository.existsByEmailAndIsDeletedFalse("dup@test.com")).thenReturn(true);

        RegisterRequest req = new RegisterRequest();
        req.setFirstName("Test");
        req.setLastName("User");
        req.setEmail("dup@test.com");
        req.setPassword("Password123");
        req.setRole("STUDENT");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> authService.register(req));
        assertTrue(ex.getMessage().contains("already exists"));
    }
}
