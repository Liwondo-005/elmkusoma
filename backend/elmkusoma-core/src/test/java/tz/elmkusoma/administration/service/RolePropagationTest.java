package tz.elmkusoma.administration.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import tz.elmkusoma.administration.dto.PeopleMemberResponse;
import tz.elmkusoma.administration.repository.InstitutionInvitationRepository;
import tz.elmkusoma.config.security.PermissionCacheService;
import tz.elmkusoma.shared.domain.InstitutionMembership;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.InstitutionMembershipRepository;
import tz.elmkusoma.shared.repository.UserRepository;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Membership role → users.role mapping and its propagation in
 * {@link InstitutionPeopleService#updateMemberRole}.
 */
@ExtendWith(MockitoExtension.class)
class RolePropagationTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private InstitutionMembershipRepository membershipRepository;
    @Mock
    private InstitutionInvitationRepository invitationRepository;
    @Mock
    private InstitutionScopeService scopeService;
    @Mock
    private PlatformPolicyService platformPolicyService;
    @Mock
    private PermissionCacheService permissionCacheService;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private InstitutionPeopleService service;

    private UUID institutionId;
    private UUID memberUserId;
    private String updatedBy;

    @BeforeEach
    void setUp() {
        institutionId = UUID.randomUUID();
        memberUserId = UUID.randomUUID();
        updatedBy = "admin@example.com";
    }

    private InstitutionMembership membership(InstitutionMembership.Role role) {
        return InstitutionMembership.builder()
                .userId(memberUserId)
                .institutionId(institutionId)
                .role(role)
                .isActive(true)
                .isDeleted(false)
                .build();
    }

    private User user(User.Role role) {
        return User.builder()
                .id(memberUserId)
                .email("member@example.com")
                .firstName("Member")
                .lastName("User")
                .role(role)
                .build();
    }

    private void stubMembership(InstitutionMembership membership) {
        when(membershipRepository.findByUserIdAndIsActiveTrue(memberUserId)).thenReturn(List.of(membership));
    }

    // ── mapMembershipToUserRole ──

    @Test
    void mapMembershipToUserRole_coversEveryMembershipRole() throws Exception {
        Map<InstitutionMembership.Role, User.Role> expected = Map.of(
                InstitutionMembership.Role.OWNER, User.Role.INSTITUTION_ADMIN,
                InstitutionMembership.Role.ADMIN, User.Role.INSTITUTION_ADMIN,
                InstitutionMembership.Role.INSTITUTION_ADMIN, User.Role.INSTITUTION_ADMIN,
                InstitutionMembership.Role.TEACHER, User.Role.TEACHER,
                InstitutionMembership.Role.STUDENT, User.Role.STUDENT,
                InstitutionMembership.Role.PARENT, User.Role.PARENT,
                InstitutionMembership.Role.OTHER_LEARNER, User.Role.OTHER_LEARNER,
                InstitutionMembership.Role.INSTRUCTOR, User.Role.INSTRUCTOR,
                InstitutionMembership.Role.NATIONAL_ADMIN, User.Role.NATIONAL_ADMIN);

        assertEquals(InstitutionMembership.Role.values().length, expected.size(),
                "mapping table must cover every membership role");

        Method method = InstitutionPeopleService.class.getDeclaredMethod(
                "mapMembershipToUserRole", InstitutionMembership.Role.class);
        method.setAccessible(true);

        for (InstitutionMembership.Role role : InstitutionMembership.Role.values()) {
            Object mapped = method.invoke(null, role);
            assertEquals(expected.get(role), mapped, "unexpected mapping for membership role " + role);
        }
    }

    // ── updateMemberRole propagation ──

    @Test
    void updateMemberRole_propagatesTeacherMembershipToUserRole() {
        InstitutionMembership membership = membership(InstitutionMembership.Role.STUDENT);
        stubMembership(membership);
        User user = user(User.Role.STUDENT);
        when(userRepository.findById(memberUserId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(scopeService.getMembershipRole(memberUserId, institutionId))
                .thenReturn(InstitutionMembership.Role.TEACHER);

        PeopleMemberResponse response = service.updateMemberRole(
                institutionId, memberUserId, "TEACHER", updatedBy);

        assertEquals(InstitutionMembership.Role.TEACHER, membership.getRole());
        assertEquals(User.Role.TEACHER, user.getRole());
        assertEquals("TEACHER", response.getMembershipRole());
        verify(userRepository).save(user);
        verify(permissionCacheService).invalidateUserPermissions(memberUserId, institutionId);
        verify(permissionCacheService).invalidateUserRole(memberUserId, institutionId);
        verify(permissionCacheService).invalidateMembership(memberUserId, institutionId);
    }

    @Test
    void updateMemberRole_adminMembershipBecomesInstitutionAdminUserRole() {
        InstitutionMembership membership = membership(InstitutionMembership.Role.STUDENT);
        stubMembership(membership);
        User user = user(User.Role.STUDENT);
        when(userRepository.findById(memberUserId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(scopeService.getMembershipRole(memberUserId, institutionId))
                .thenReturn(InstitutionMembership.Role.ADMIN);

        service.updateMemberRole(institutionId, memberUserId, "ADMIN", updatedBy);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals(User.Role.INSTITUTION_ADMIN, captor.getValue().getRole());
    }

    @Test
    void updateMemberRole_ownerMembershipBecomesInstitutionAdminUserRole() {
        InstitutionMembership membership = membership(InstitutionMembership.Role.TEACHER);
        stubMembership(membership);
        User user = user(User.Role.TEACHER);
        when(userRepository.findById(memberUserId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(scopeService.getMembershipRole(memberUserId, institutionId))
                .thenReturn(InstitutionMembership.Role.OWNER);

        service.updateMemberRole(institutionId, memberUserId, "OWNER", updatedBy);

        assertEquals(User.Role.INSTITUTION_ADMIN, user.getRole());
    }

    @Test
    void updateMemberRole_acceptsLowerCaseRoleName() {
        InstitutionMembership membership = membership(InstitutionMembership.Role.STUDENT);
        stubMembership(membership);
        User user = user(User.Role.STUDENT);
        when(userRepository.findById(memberUserId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(scopeService.getMembershipRole(memberUserId, institutionId))
                .thenReturn(InstitutionMembership.Role.OTHER_LEARNER);

        service.updateMemberRole(institutionId, memberUserId, "other_learner", updatedBy);

        assertEquals(InstitutionMembership.Role.OTHER_LEARNER, membership.getRole());
        assertEquals(User.Role.OTHER_LEARNER, user.getRole());
    }

    @Test
    void updateMemberRole_preservesNationalAdminPlatformRole() {
        InstitutionMembership membership = membership(InstitutionMembership.Role.NATIONAL_ADMIN);
        stubMembership(membership);
        User user = user(User.Role.NATIONAL_ADMIN);
        when(userRepository.findById(memberUserId)).thenReturn(Optional.of(user));

        service.updateMemberRole(institutionId, memberUserId, "TEACHER", updatedBy);

        assertEquals(InstitutionMembership.Role.TEACHER, membership.getRole(),
                "the institution membership must still be downgraded");
        assertEquals(User.Role.NATIONAL_ADMIN, user.getRole(),
                "a platform NATIONAL_ADMIN must not lose users.role through a membership change");
        verify(userRepository, never()).save(any(User.class));
        verify(permissionCacheService).invalidateUserRole(memberUserId, institutionId);
    }

    @Test
    void updateMemberRole_doesNotRewriteUserRoleWhenAlreadyConsistent() {
        InstitutionMembership membership = membership(InstitutionMembership.Role.STUDENT);
        stubMembership(membership);
        User user = user(User.Role.TEACHER);
        when(userRepository.findById(memberUserId)).thenReturn(Optional.of(user));
        when(scopeService.getMembershipRole(memberUserId, institutionId))
                .thenReturn(InstitutionMembership.Role.TEACHER);

        service.updateMemberRole(institutionId, memberUserId, "TEACHER", updatedBy);

        assertEquals(User.Role.TEACHER, user.getRole());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateMemberRole_unknownMembershipRoleIsRejected() {
        InstitutionMembership membership = membership(InstitutionMembership.Role.STUDENT);
        stubMembership(membership);

        assertThrows(IllegalArgumentException.class, () ->
                service.updateMemberRole(institutionId, memberUserId, "SUPER_ADMIN", updatedBy));

        assertEquals(InstitutionMembership.Role.STUDENT, membership.getRole(),
                "the membership must be untouched when the role is invalid");
        verify(membershipRepository, never()).save(any());
        verify(userRepository, never()).findById(any());
    }

    @Test
    void updateMemberRole_memberWithoutActiveMembershipIsRejected() {
        when(membershipRepository.findByUserIdAndIsActiveTrue(memberUserId)).thenReturn(List.of());

        assertThrows(RuntimeException.class, () ->
                service.updateMemberRole(institutionId, memberUserId, "TEACHER", updatedBy));

        verify(userRepository, never()).save(any());
        verify(permissionCacheService, never()).invalidateUserRole(any(), any());
    }

    @Test
    void updateMemberRole_membershipOfAnotherInstitutionIsRejected() {
        InstitutionMembership foreign = InstitutionMembership.builder()
                .userId(memberUserId)
                .institutionId(UUID.randomUUID())
                .role(InstitutionMembership.Role.TEACHER)
                .isActive(true)
                .isDeleted(false)
                .build();
        stubMembership(foreign);

        assertThrows(RuntimeException.class, () ->
                service.updateMemberRole(institutionId, memberUserId, "STUDENT", updatedBy));

        assertEquals(InstitutionMembership.Role.TEACHER, foreign.getRole());
        verify(userRepository, never()).findById(any());
        verify(membershipRepository, never()).save(any());
    }
}
