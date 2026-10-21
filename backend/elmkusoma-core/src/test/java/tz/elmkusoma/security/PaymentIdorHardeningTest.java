package tz.elmkusoma.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.bundle.domain.Bundle;
import tz.elmkusoma.bundle.domain.BundleItem;
import tz.elmkusoma.bundle.repository.BundleItemRepository;
import tz.elmkusoma.bundle.repository.BundleRepository;
import tz.elmkusoma.common.BaseEntity;
import tz.elmkusoma.learner.domain.LearnerNotification;
import tz.elmkusoma.learner.repository.LearnerNotificationRepository;
import tz.elmkusoma.parent.domain.Entitlement;
import tz.elmkusoma.parent.domain.Parent;
import tz.elmkusoma.parent.domain.ParentStudentLink;
import tz.elmkusoma.parent.domain.Payment;
import tz.elmkusoma.parent.repository.EntitlementRepository;
import tz.elmkusoma.parent.repository.ParentRepository;
import tz.elmkusoma.parent.repository.ParentStudentLinkRepository;
import tz.elmkusoma.parent.repository.PaymentRepository;
import tz.elmkusoma.parent.service.PaymentEventNotificationService;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.student.domain.Student;
import tz.elmkusoma.student.repository.StudentRepository;
import tz.elmkusoma.testutil.TestDataSeeder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Two money-adjacent endpoints that took an identifier belonging to somebody else.
 *
 * <p>Both were reachable by any authenticated user of the right role and both were addressed by a
 * plain UUID, which is the shape that turns into an IDOR the moment such an id can be guessed,
 * leaked, or read out of a list response. What each one leaked differed - one let a caller clear
 * another family's inbox, the other let a caller price a learner they had no relationship with and
 * learn which services that learner already holds - but the cause and the fix are the same, so
 * they are pinned together.</p>
 *
 * <p>The security property under test is stronger than "the other row is not modified". A 403 or a
 * distinct "forbidden" body would confirm that an id exists and belongs to someone, which is
 * itself the leak. A caller must therefore be unable to tell a row that belongs to another family
 * apart from an id that was never issued at all.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestDataSeeder.class)
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class PaymentIdorHardeningTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private ParentRepository parentRepository;
    @Autowired private ParentStudentLinkRepository linkRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private LearnerNotificationRepository notificationRepository;
    @Autowired private EntitlementRepository entitlementRepository;
    @Autowired private BundleRepository bundleRepository;
    @Autowired private BundleItemRepository bundleItemRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private PaymentEventNotificationService paymentEventNotificationService;

    private final List<UUID> createdUsers = new ArrayList<>();
    private final List<Payment> createdPayments = new ArrayList<>();

    private UUID institutionId;
    private UUID myUserId;
    private UUID myLearnerUserId;
    private Parent me;
    private Parent otherParent;
    private Student myChild;
    private Student unlinkedChild;
    private Student myLearnerSelf;
    private LearnerNotification myNotification;
    private LearnerNotification theirNotification;
    private Bundle bundle;
    private UUID coveredService;

    @BeforeEach
    void setUp() {
        institutionId = TestDataSeeder.INSTITUTION_ID;

        myUserId = newUser(User.Role.PARENT);
        myLearnerUserId = newUser(User.Role.STUDENT);
        UUID otherUserId = newUser(User.Role.PARENT);

        me = parentRepository.save(withInstitution(Parent.builder().userId(myUserId).build(), institutionId));
        otherParent = parentRepository.save(withInstitution(Parent.builder().userId(otherUserId).build(), institutionId));

        myChild = newStudent();
        unlinkedChild = newStudent();
        myLearnerSelf = newStudent(myLearnerUserId);

        link(me, myChild);
        link(otherParent, unlinkedChild);

        myNotification = notification(myUserId);
        theirNotification = notification(otherParent.getUserId());

        coveredService = UUID.randomUUID();
        bundle = withInstitution(Bundle.builder()
                .name("IDOR probe bundle")
                .description("covers an item so no price is needed")
                .serviceType("BUNDLE_IDOR_" + UUID.randomUUID())
                .accessDays(30)
                .status(Bundle.STATUS_PUBLISHED)
                .build(), institutionId);
        bundle = bundleRepository.saveAndFlush(bundle);
        bundleItemRepository.saveAndFlush(withInstitution(BundleItem.builder()
                .bundleId(bundle.getId())
                .serviceType("COURSE_ACCESS")
                .serviceId(coveredService)
                .position(0)
                .build(), institutionId));

        // Already-held access, so the quote resolves without ever consulting a pricing policy.
        entitlementRepository.saveAndFlush(withInstitution(Entitlement.builder()
                .userId(myUserId)
                .studentId(myChild.getId())
                .serviceType("COURSE_ACCESS")
                .serviceId(coveredService)
                .status("ACTIVE")
                .startsAt(LocalDateTime.now().minusDays(1))
                .expiresAt(LocalDateTime.now().plusDays(10))
                .build(), institutionId));
    }

    @AfterEach
    void tearDown() {
        bundleItemRepository.deleteAll();
        bundleRepository.deleteAll();
        paymentRepository.deleteAll(createdPayments);
        entitlementRepository.deleteAll();
        notificationRepository.deleteAll(List.of(myNotification, theirNotification));
        linkRepository.deleteAll();
        studentRepository.deleteAll(List.of(myChild, unlinkedChild, myLearnerSelf));
        parentRepository.deleteAll(List.of(me, otherParent));
        createdUsers.forEach(userRepository::deleteById);
    }

    // ------------------------------------------- marking another family's inbox read

    @Test
    @WithMockUser(roles = "PARENT")
    void aParentCanMarkTheirOwnNotificationRead() throws Exception {
        mockMvc.perform(put("/v1/my/notifications/{id}/read", myNotification.getId())
                        .requestAttr("userId", myUserId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        assertTrue(notificationRepository.findById(myNotification.getId()).orElseThrow().getIsRead(),
                "the caller's own notification must be marked read");
    }

    @Test
    @WithMockUser(roles = "PARENT")
    void oneFamilyCannotMarkAnotherFamilysNotificationRead() throws Exception {
        // The defect: the handler looked the row up by id alone and wrote to it, so any parent who
        // could name another family's notification id simply cleared it, silently, on their side.
        mockMvc.perform(put("/v1/my/notifications/{id}/read", theirNotification.getId())
                        .requestAttr("userId", myUserId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        assertFalse(Boolean.TRUE.equals(
                        notificationRepository.findById(theirNotification.getId()).orElseThrow().getIsRead()),
                "another family's notification must not be marked read by this caller");
    }

    @Test
    @WithMockUser(roles = "PARENT")
    void aForeignIdAndAnIdThatNeverExistedAreIndistinguishable() throws Exception {
        // If a foreign id answered differently from a made-up one, the endpoint would tell an
        // attacker which ids are real, which is the half of the leak that survives the fix above.
        int foreign = mockMvc.perform(put("/v1/my/notifications/{id}/read", theirNotification.getId())
                        .requestAttr("userId", myUserId))
                .andReturn().getResponse().getStatus();
        int missing = mockMvc.perform(put("/v1/my/notifications/{id}/read", UUID.randomUUID())
                        .requestAttr("userId", myUserId))
                .andReturn().getResponse().getStatus();

        assertEquals404(foreign);
        assertEquals404(missing);
    }

    @Test
    @WithMockUser(roles = "PARENT")
    void markingReadIsIdempotentForTheOwnersRow() throws Exception {
        mockMvc.perform(put("/v1/my/notifications/{id}/read", myNotification.getId())
                        .requestAttr("userId", myUserId))
                .andExpect(status().isOk());
        mockMvc.perform(put("/v1/my/notifications/{id}/read", myNotification.getId())
                        .requestAttr("userId", myUserId))
                .andExpect(status().isOk());

        assertTrue(notificationRepository.findById(myNotification.getId()).orElseThrow().getIsRead(),
                "re-marking a notification the caller already owns must stay a success");
    }

    // ------------------------------------------- pricing a learner nobody is linked to

    @Test
    @WithMockUser(roles = "PARENT")
    void aParentMayQuoteForTheirOwnLinkedChild() throws Exception {
        mockMvc.perform(get("/v1/bundles/{id}/quote", bundle.getId())
                        .requestAttr("userId", myUserId)
                        .param("studentId", myChild.getId().toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fullyCovered").value(true));
    }

    @Test
    @WithMockUser(roles = "PARENT")
    void aParentCannotQuoteForAChildBelongingToAnotherFamily() throws Exception {
        // A child who is linked to somebody else. Refusing this caller must not depend on the
        // child being linked to a parent this caller happens to know about.
        mockMvc.perform(get("/v1/bundles/{id}/quote", bundle.getId())
                        .requestAttr("userId", myUserId)
                        .param("studentId", unlinkedChild.getId().toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "PARENT")
    void aParentCannotQuoteForAChildLinkedToNobody() throws Exception {
        // A learner record with no parent at all - the case a "not my child" check written as a
        // lookup of the caller's own links would answer wrongly for, if it answered for others.
        Student stranger = newStudent();
        mockMvc.perform(get("/v1/bundles/{id}/quote", bundle.getId())
                        .requestAttr("userId", myUserId)
                        .param("studentId", stranger.getId().toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "LEARNER")
    void aLearnerMayQuoteForThemselves() throws Exception {
        // Same entitlement trick, keyed on the learner's own record this time.
        entitlementRepository.saveAndFlush(withInstitution(Entitlement.builder()
                .userId(myLearnerUserId)
                .studentId(myLearnerSelf.getId())
                .serviceType("COURSE_ACCESS")
                .serviceId(coveredService)
                .status("ACTIVE")
                .startsAt(LocalDateTime.now().minusDays(1))
                .expiresAt(LocalDateTime.now().plusDays(10))
                .build(), institutionId));

        mockMvc.perform(get("/v1/bundles/{id}/quote", bundle.getId())
                        .requestAttr("userId", myLearnerUserId)
                        .param("studentId", myLearnerSelf.getId().toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fullyCovered").value(true));
    }

    @Test
    @WithMockUser(roles = "LEARNER")
    void aLearnerCannotPriceAnotherLearner() throws Exception {
        mockMvc.perform(get("/v1/bundles/{id}/quote", bundle.getId())
                        .requestAttr("userId", myLearnerUserId)
                        .param("studentId", myChild.getId().toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void anAnonymousCallerIsRefusedBeforeAnyOwnershipQuestion() throws Exception {
        mockMvc.perform(get("/v1/bundles/{id}/quote", bundle.getId())
                        .param("studentId", myChild.getId().toString()))
                .andExpect(result -> {
                    int code = result.getResponse().getStatus();
                    assertTrue(code == 401 || code == 403,
                            "an unauthenticated caller must never reach the quote, got " + code);
                });
    }

    // ------------------------------------------- the clock is no longer a parameter

    @Test
    @WithMockUser(roles = "PARENT")
    void theQuoteIsAlwaysPricedAgainstNow_noMatterWhatAtIsPassed() throws Exception {
        // The endpoint used to accept an arbitrary timestamp and price the learner's entitlements
        // against it, which would let a caller be quoted as though their access had already
        // lapsed or had not started. No caller needs it: the pricing tests drive the service
        // directly, where the clock is an ordinary argument. The entitlement here is live only
        // from yesterday, so an override that still worked would price against a time before it
        // began and the bundle would not come back covered.
        mockMvc.perform(get("/v1/bundles/{id}/quote", bundle.getId())
                        .requestAttr("userId", myUserId)
                        .param("studentId", myChild.getId().toString())
                        .param("at", LocalDateTime.now().minusDays(30).toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fullyCovered").value(true));
    }

    // ------------------------------------- a notification can never open a door by itself

    @Test
    @WithMockUser(roles = "PARENT")
    void aMoneyNotificationOnItsOwnGrantsNoAccess() throws Exception {
        // Spec 61 asks that notifications never independently grant access. That property is
        // only worth asserting if the inbox write is driven through the real service against
        // the real database, because the interesting failure is a side effect nobody wrote
        // deliberately - an entitlement row, a status flip, a seat - rather than the alert.
        //
        // The service reached here is one the parent cannot call over HTTP. The point is not
        // that an attacker can emit an alert; it is that emitting one changes nothing about
        // what the family can reach.
        UUID uncoveredService = UUID.randomUUID();
        Entitlement heldBefore = entitlementRepository.findAll().stream()
                .filter(entitlement -> coveredService.equals(entitlement.getServiceId()))
                .findFirst().orElseThrow();

        Payment payment = paymentRepository.saveAndFlush(withInstitution(Payment.builder()
                .parentId(me.getId())
                .amount(new BigDecimal("15000"))
                .currency("TZS")
                .serviceType("COURSE_ACCESS")
                .serviceId(uncoveredService)
                .status("COMPLETED")
                .build(), institutionId));
        createdPayments.add(payment);

        paymentEventNotificationService.paymentInitiated(payment);

        assertTrue(notificationRepository.existsByUserIdAndTargetIdAndNotificationTypeAndTitleAndIsDeletedFalse(
                        myUserId, payment.getId(), PaymentEventNotificationService.TYPE_PAYMENT,
                        PaymentEventNotificationService.TITLE_PAYMENT_INITIATED),
                "the payer must actually be told about the payment, or this proves nothing");

        // Access is still read from the entitlement, and only from the entitlement.
        mockMvc.perform(get("/v1/bundles/{id}/quote", bundle.getId())
                        .requestAttr("userId", myUserId)
                        .param("studentId", myChild.getId().toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fullyCovered").value(true));

        Entitlement heldAfter = entitlementRepository.findById(heldBefore.getId()).orElseThrow();
        assertTrue("ACTIVE".equals(heldAfter.getStatus())
                        && heldBefore.getExpiresAt().equals(heldAfter.getExpiresAt()),
                "an inbox row must not extend, shorten or revoke access that already exists");

        assertTrue(entitlementRepository.findAll().stream()
                        .noneMatch(entitlement -> uncoveredService.equals(entitlement.getServiceId())),
                "writing an inbox row must not create an entitlement for anything");
    }

    // ------------------------------------------------------------------ helpers

    private static void assertEquals404(int code) {
        assertTrue(code == 404, "expected the same 404 for both, got " + code);
    }

    private LearnerNotification notification(UUID userId) {
        return notificationRepository.save(withInstitution(LearnerNotification.builder()
                .userId(userId)
                .title("IDOR probe")
                .message("a row that belongs to exactly one family")
                .notificationType("PROBE")
                .isRead(false)
                .build(), institutionId));
    }

    private UUID newUser(User.Role role) {
        User saved = userRepository.save(withInstitution(User.builder()
                .email(role.name().toLowerCase() + "-" + UUID.randomUUID() + "@test.go.tz")
                .firstName("Test")
                .lastName("User")
                .passwordHash("x")
                .role(role)
                .isActive(true)
                .build(), institutionId));
        createdUsers.add(saved.getId());
        return saved.getId();
    }

    private Student newStudent() {
        return newStudent(newUser(User.Role.STUDENT));
    }

    private Student newStudent(UUID userId) {
        return studentRepository.save(withInstitution(Student.builder()
                .userId(userId)
                .admissionNumber("ADM-" + UUID.randomUUID())
                .build(), institutionId));
    }

    private void link(Parent parent, Student student) {
        linkRepository.save(withInstitution(ParentStudentLink.builder()
                .parentId(parent.getId())
                .studentId(student.getId())
                .build(), institutionId));
    }

    private static <T extends BaseEntity> T withInstitution(T entity, UUID id) {
        entity.setInstitutionId(id);
        return entity;
    }
}
