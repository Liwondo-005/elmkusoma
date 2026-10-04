package tz.elmkusoma.security;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import tz.elmkusoma.parent.domain.Parent;
import tz.elmkusoma.parent.domain.Payment;
import tz.elmkusoma.parent.repository.ParentRepository;
import tz.elmkusoma.parent.repository.PaymentRepository;
import tz.elmkusoma.shared.domain.Institution;
import tz.elmkusoma.shared.domain.InstitutionMembership;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.InstitutionMembershipRepository;
import tz.elmkusoma.shared.repository.InstitutionRepository;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.student.domain.Student;
import tz.elmkusoma.student.domain.StudentStatus;
import tz.elmkusoma.student.repository.StudentRepository;
import tz.elmkusoma.testutil.TestTokens;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP security matrix for the recently-hardened payment refund/cancel routes —
 * through the real filter chain (JwtAuthenticationFilter →
 * JwtRequestAttributeFilter → OrganizationContextResolver → method security →
 * GlobalExceptionHandler).
 *
 * <p>Contract under test:</p>
 * <ul>
 *   <li>refund ({@code POST /v1/parents/payments/{id}/refund}): INSTITUTION_ADMIN
 *       of institution A refunding B's COMPLETED payment → 403 with the payment
 *       status unchanged; platform ADMIN cross-institution → 200 REFUNDED;
 *       refunding a non-COMPLETED payment → 409 with the status preserved.</li>
 *   <li>cancel ({@code POST /v1/my/payments/{id}/cancel}): non-owner parent →
 *       rejected with the payment untouched; owner parent → 200 CANCELLED.</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PaymentRefundHttpSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InstitutionRepository institutionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InstitutionMembershipRepository membershipRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ParentRepository parentRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    private Institution instA;
    private Institution instB;
    private User instAdminA;
    private User platformAdmin;
    private User parentUserB;
    private User parentUserB2;
    private Parent parentB;
    private Parent parentB2;
    private Student studentB;
    private Payment payRefundCross;
    private Payment payRefundAdmin;
    private Payment payRefundPending;
    private Payment payCancelOwner;
    private Payment payCancelForeign;

    @BeforeAll
    void createTwoInstitutionFixtures() {
        String run = UUID.randomUUID().toString().substring(0, 8);

        instA = saveInstitution("SecPay-A " + run);
        instB = saveInstitution("SecPay-B " + run);

        instAdminA = saveUser("secpay-admin-a-" + run + "@test.com", User.Role.INSTITUTION_ADMIN, instA);
        platformAdmin = saveUser("secpay-admin-" + run + "@test.com", User.Role.ADMIN, instA);

        parentUserB = saveUser("secpay-parent-b-" + run + "@test.com", User.Role.PARENT, instB);
        parentB = saveParent(parentUserB, instB);
        parentUserB2 = saveUser("secpay-parent-b2-" + run + "@test.com", User.Role.PARENT, instB);
        parentB2 = saveParent(parentUserB2, instB);
        User studentUserB = saveUser("secpay-student-b-" + run + "@test.com", User.Role.STUDENT, instB);
        studentB = studentRepository.save(Student.builder()
                .userId(studentUserB.getId())
                .admissionNumber("SECPAY-B-" + run)
                .status(StudentStatus.ACTIVE)
                .enrollmentDate(LocalDate.now())
                .institutionId(instB.getId())
                .build());

        // dedicated rows per mutation so tests stay order-independent
        payRefundCross = savePayment(parentB, "COMPLETED", "secpay-cross-" + run);
        payRefundAdmin = savePayment(parentB, "COMPLETED", "secpay-admin-" + run);
        payRefundPending = savePayment(parentB, "PENDING", "secpay-pending-" + run);
        payCancelOwner = savePayment(parentB, "PENDING", "secpay-owner-" + run);
        payCancelForeign = savePayment(parentB, "PENDING", "secpay-foreign-" + run);
    }

    // ── fixtures ──

    private Institution saveInstitution(String name) {
        return institutionRepository.save(Institution.builder()
                .name(name)
                .code("SECPAY-" + UUID.randomUUID().toString().substring(0, 8))
                .type(Institution.InstitutionType.SECONDARY)
                .country("Tanzania")
                .isActive(true)
                .isDeleted(false)
                .build());
    }

    private User saveUser(String email, User.Role role, Institution institution) {
        User user = User.builder()
                .email(email)
                .passwordHash("test-hash")
                .firstName("SecPay")
                .lastName("Tester")
                .role(role)
                .isActive(true)
                .isEmailVerified(true)
                .isDeleted(false)
                .build();
        user.setInstitutionId(institution.getId());
        User saved = userRepository.save(user);
        InstitutionMembership.Role membershipRole = switch (role) {
            case TEACHER -> InstitutionMembership.Role.TEACHER;
            case PARENT -> InstitutionMembership.Role.PARENT;
            case STUDENT, OTHER_LEARNER -> InstitutionMembership.Role.STUDENT;
            case INSTITUTION_ADMIN, ADMIN -> InstitutionMembership.Role.ADMIN;
            default -> InstitutionMembership.Role.STUDENT;
        };
        membershipRepository.save(InstitutionMembership.builder()
                .userId(saved.getId())
                .institutionId(institution.getId())
                .role(membershipRole)
                .isActive(true)
                .build());
        return saved;
    }

    private Parent saveParent(User user, Institution institution) {
        Parent parent = Parent.builder()
                .userId(user.getId())
                .relationshipType(Parent.RelationshipType.GUARDIAN)
                .build();
        parent.setInstitutionId(institution.getId());
        return parentRepository.save(parent);
    }

    private Payment savePayment(Parent parent, String paymentStatus, String description) {
        Payment payment = Payment.builder()
                .parentId(parent.getId())
                .studentId(studentB.getId())
                .institutionId(instB.getId())
                .amount(new BigDecimal("50000"))
                .currency("TZS")
                .serviceType("TUITION")
                .description(description)
                .status(paymentStatus)
                .build();
        if ("COMPLETED".equals(paymentStatus)) {
            payment.setPaidAt(LocalDateTime.now().minusHours(1));
        }
        return paymentRepository.save(payment);
    }

    private String token(User user) {
        return TestTokens.userToken(user.getEmail());
    }

    private String paymentStatus(UUID paymentId) {
        return paymentRepository.findById(paymentId).orElseThrow().getStatus();
    }

    // ── refund ──

    @Test
    void institutionAdmin_refundsForeignCompletedPayment_403_statusUnchanged() throws Exception {
        mockMvc.perform(post("/v1/parents/payments/" + payRefundCross.getId() + "/refund")
                        .header("Authorization", "Bearer " + token(instAdminA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"duplicate charge\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        // denied mutation must not write
        assertEquals("COMPLETED", paymentStatus(payRefundCross.getId()));
    }

    @Test
    void platformAdmin_refundsForeignCompletedPayment_200_refunded() throws Exception {
        mockMvc.perform(post("/v1/parents/payments/" + payRefundAdmin.getId() + "/refund")
                        .header("Authorization", "Bearer " + token(platformAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"duplicate charge\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.paymentId").value(payRefundAdmin.getId().toString()))
                .andExpect(jsonPath("$.data.status").value("REFUNDED"));

        assertEquals("REFUNDED", paymentStatus(payRefundAdmin.getId()));
    }

    @Test
    void admin_refundNonCompletedPayment_409_statusPreserved() throws Exception {
        mockMvc.perform(post("/v1/parents/payments/" + payRefundPending.getId() + "/refund")
                        .header("Authorization", "Bearer " + token(platformAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"duplicate charge\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));

        // the state-machine error must not change the payment
        assertEquals("PENDING", paymentStatus(payRefundPending.getId()));
    }

    // ── cancel ──

    @Test
    void parent_cancelNonOwnedPayment_rejected_statusUnchanged() throws Exception {
        // Non-owner cancel is denied with 403 (ForbiddenException); the
        // payment must remain untouched.
        mockMvc.perform(post("/v1/my/payments/" + payCancelForeign.getId() + "/cancel")
                        .header("Authorization", "Bearer " + token(parentUserB2)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        assertEquals("PENDING", paymentStatus(payCancelForeign.getId()));
    }

    @Test
    void parent_cancelOwnPayment_200_cancelled() throws Exception {
        mockMvc.perform(post("/v1/my/payments/" + payCancelOwner.getId() + "/cancel")
                        .header("Authorization", "Bearer " + token(parentUserB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.paymentId").value(payCancelOwner.getId().toString()))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        assertEquals("CANCELLED", paymentStatus(payCancelOwner.getId()));
    }
}
