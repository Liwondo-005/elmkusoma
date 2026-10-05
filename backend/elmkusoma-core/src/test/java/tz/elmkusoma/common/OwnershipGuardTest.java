package tz.elmkusoma.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tz.elmkusoma.exception.ForbiddenException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * OwnershipGuard must fail closed: a null on either side of a check means
 * ownership cannot be proven, so access is denied — never silently allowed.
 */
class OwnershipGuardTest {

    private OwnershipGuard guard;
    private UUID institutionA;
    private UUID institutionB;
    private UUID userA;
    private UUID userB;

    @BeforeEach
    void setUp() {
        guard = new OwnershipGuard();
        institutionA = UUID.randomUUID();
        institutionB = UUID.randomUUID();
        userA = UUID.randomUUID();
        userB = UUID.randomUUID();
    }

    @Test
    void verifyInstitution_allowsMatchingInstitutions() {
        assertDoesNotThrow(() -> guard.verifyInstitution(institutionA, institutionA));
    }

    @Test
    void verifyInstitution_deniesMismatch() {
        assertThrows(ForbiddenException.class,
                () -> guard.verifyInstitution(institutionA, institutionB));
    }

    @Test
    void verifyInstitution_deniesNullResourceInstitution() {
        assertThrows(ForbiddenException.class,
                () -> guard.verifyInstitution(null, institutionA));
    }

    @Test
    void verifyInstitution_deniesNullRequestInstitution() {
        assertThrows(ForbiddenException.class,
                () -> guard.verifyInstitution(institutionA, null));
    }

    @Test
    void verifyUser_allowsMatchingUsers() {
        assertDoesNotThrow(() -> guard.verifyUser(userA, userA));
    }

    @Test
    void verifyUser_deniesMismatch() {
        assertThrows(ForbiddenException.class, () -> guard.verifyUser(userA, userB));
    }

    @Test
    void verifyUser_deniesNullEitherSide() {
        assertThrows(ForbiddenException.class, () -> guard.verifyUser(null, userA));
        assertThrows(ForbiddenException.class, () -> guard.verifyUser(userA, null));
    }

    @Test
    void verifyProvider_allowsMatchingProviders() {
        assertDoesNotThrow(() -> guard.verifyProvider(userA, userA));
    }

    @Test
    void verifyProvider_deniesMismatchAndNull() {
        assertThrows(ForbiddenException.class, () -> guard.verifyProvider(userA, userB));
        assertThrows(ForbiddenException.class, () -> guard.verifyProvider(null, userA));
        assertThrows(ForbiddenException.class, () -> guard.verifyProvider(userA, null));
    }
}
