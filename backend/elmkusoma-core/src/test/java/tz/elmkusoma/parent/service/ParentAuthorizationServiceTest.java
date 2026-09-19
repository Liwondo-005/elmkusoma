package tz.elmkusoma.parent.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tz.elmkusoma.exception.ForbiddenException;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.parent.domain.Parent;
import tz.elmkusoma.parent.repository.ParentRepository;
import tz.elmkusoma.parent.repository.ParentStudentLinkRepository;
import tz.elmkusoma.shared.repository.InstitutionMembershipRepository;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParentAuthorizationServiceTest {

    @Mock private ParentRepository parentRepository;
    @Mock private ParentStudentLinkRepository studentLinkRepository;
    @Mock private InstitutionMembershipRepository membershipRepository;

    @InjectMocks
    private ParentAuthorizationService authService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID PARENT_ID = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID INSTITUTION_ID = UUID.randomUUID();

    private Parent buildParent() {
        Parent parent = new Parent();
        parent.setId(PARENT_ID);
        parent.setUserId(USER_ID);
        parent.setInstitutionId(INSTITUTION_ID);
        return parent;
    }

    @Test
    void resolveParent_returnsParent_whenExists() {
        when(parentRepository.findByUserIdAndIsDeletedFalse(USER_ID)).thenReturn(Optional.of(buildParent()));
        Parent result = authService.resolveParent(USER_ID);
        assertNotNull(result);
        assertEquals(PARENT_ID, result.getId());
    }

    @Test
    void resolveParent_throws_whenNotFound() {
        when(parentRepository.findByUserIdAndIsDeletedFalse(USER_ID)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> authService.resolveParent(USER_ID));
    }

    @Test
    void requireChildAccess_returnsParent_whenAuthorized() {
        when(parentRepository.findByUserIdAndIsDeletedFalse(USER_ID)).thenReturn(Optional.of(buildParent()));
        when(studentLinkRepository.existsByParentIdAndStudentIdAndIsDeletedFalse(PARENT_ID, STUDENT_ID)).thenReturn(true);
        Parent result = authService.requireChildAccess(USER_ID, STUDENT_ID);
        assertNotNull(result);
        assertEquals(PARENT_ID, result.getId());
    }

    @Test
    void requireChildAccess_throwsForbidden_whenNotLinked() {
        when(parentRepository.findByUserIdAndIsDeletedFalse(USER_ID)).thenReturn(Optional.of(buildParent()));
        when(studentLinkRepository.existsByParentIdAndStudentIdAndIsDeletedFalse(PARENT_ID, STUDENT_ID)).thenReturn(false);
        assertThrows(ForbiddenException.class, () -> authService.requireChildAccess(USER_ID, STUDENT_ID));
    }

    @Test
    void isAuthorizedChild_returnsTrue_whenLinked() {
        when(parentRepository.findByUserIdAndIsDeletedFalse(USER_ID)).thenReturn(Optional.of(buildParent()));
        when(studentLinkRepository.existsByParentIdAndStudentIdAndIsDeletedFalse(PARENT_ID, STUDENT_ID)).thenReturn(true);
        assertTrue(authService.isAuthorizedChild(USER_ID, STUDENT_ID));
    }

    @Test
    void isAuthorizedChild_returnsFalse_whenNotLinked() {
        when(parentRepository.findByUserIdAndIsDeletedFalse(USER_ID)).thenReturn(Optional.of(buildParent()));
        when(studentLinkRepository.existsByParentIdAndStudentIdAndIsDeletedFalse(PARENT_ID, STUDENT_ID)).thenReturn(false);
        assertFalse(authService.isAuthorizedChild(USER_ID, STUDENT_ID));
    }

    @Test
    void requireInstitutionMembership_throwsForbidden_whenNotMember() {
        when(membershipRepository.existsByUserIdAndInstitutionIdAndIsActiveTrue(USER_ID, INSTITUTION_ID)).thenReturn(false);
        assertThrows(ForbiddenException.class, () -> authService.requireInstitutionMembership(USER_ID, INSTITUTION_ID));
    }

    @Test
    void requireInstitutionMembership_noOp_whenInstitutionIdNull() {
        assertDoesNotThrow(() -> authService.requireInstitutionMembership(USER_ID, null));
    }
}
