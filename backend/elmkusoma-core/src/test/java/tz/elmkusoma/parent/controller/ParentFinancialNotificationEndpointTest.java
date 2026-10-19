package tz.elmkusoma.parent.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.common.BaseEntity;
import tz.elmkusoma.parent.domain.Parent;
import tz.elmkusoma.parent.domain.ParentNotificationPreference;
import tz.elmkusoma.parent.domain.ParentStudentLink;
import tz.elmkusoma.parent.domain.Payment;
import tz.elmkusoma.parent.repository.ParentNotificationPreferenceRepository;
import tz.elmkusoma.parent.repository.ParentRepository;
import tz.elmkusoma.parent.repository.ParentStudentLinkRepository;
import tz.elmkusoma.parent.repository.PaymentRepository;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Parent financial notifications, proven through the endpoint a parent actually calls.
 *
 * <p>The assembler that builds these items was already well tested in isolation, and that turned
 * out to be exactly the problem: {@code GET /v1/my/notifications} read learner notifications
 * straight from its own repository and never consulted the assembler at all, so a parent could pay
 * for their child's access and never be told. Unit tests on a standalone component cannot catch
 * that - only asserting on the API response can.</p>
 *
 * <p>Two separate exposures matter and are pinned separately: another family's money, and this
 * family's money for a child this parent has not been verified as linked to.</p>
 *
 * <p>The response is parsed in Java rather than with JsonPath filter expressions, because
 * {@code [?(...)]} yields an empty array when nothing matches and an empty array is not null - so
 * {@code .exists()} would pass on the exact failure it is meant to detect.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestDataSeeder.class)
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class ParentFinancialNotificationEndpointTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ParentRepository parentRepository;
    @Autowired private ParentStudentLinkRepository linkRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private ParentNotificationPreferenceRepository preferenceRepository;
    @Autowired private UserRepository userRepository;

    private final List<UUID> createdUsers = new ArrayList<>();

    private UUID institutionId;
    private UUID myUserId;
    private Parent me;
    private Student myChild;
    private Student unlinkedChild;
    private Parent otherParent;
    private Student otherChild;
    private Payment myPayment;
    private Payment theirPayment;
    private Payment unlinkedChildPayment;

    @BeforeEach
    void setUp() {
        institutionId = TestDataSeeder.INSTITUTION_ID;

        myUserId = newUser(User.Role.PARENT);
        UUID otherUserId = newUser(User.Role.PARENT);
        me = parentRepository.save(withInstitution(Parent.builder().userId(myUserId).build(), institutionId));
        otherParent = parentRepository.save(withInstitution(Parent.builder().userId(otherUserId).build(), institutionId));

        myChild = newStudent();
        unlinkedChild = newStudent();
        otherChild = newStudent();

        link(me, myChild);
        link(otherParent, otherChild);

        myPayment = payment(me.getId(), myChild.getId(), "RCPT-MINE");
        theirPayment = payment(otherParent.getId(), otherChild.getId(), "RCPT-THEIRS");
        unlinkedChildPayment = payment(me.getId(), unlinkedChild.getId(), "RCPT-UNLINKED");
    }

    @AfterEach
    void tearDown() {
        linkRepository.deleteAll();
        paymentRepository.deleteAll();
        preferenceRepository.deleteAll();
        studentRepository.deleteAll(List.of(myChild, unlinkedChild, otherChild));
        parentRepository.deleteAll(List.of(me, otherParent));
        createdUsers.forEach(userRepository::deleteById);
    }

    // ------------------------------------------------------------------ the wiring itself

    @Test
    @WithMockUser(roles = "PARENT")
    void aCompletedPaymentForAChildReachesThatParentsInbox() throws Exception {
        JsonNode data = notifications(myUserId);

        assertTrue(containsId(data, "payment-" + myPayment.getId()),
                "a completed payment must reach the parent's inbox through the endpoint; got: "
                        + ids(data));
        assertTrue(containsId(data, "receipt-" + myPayment.getId()),
                "the receipt for that payment must reach the inbox too; got: " + ids(data));
    }

    @Test
    @WithMockUser(roles = "PARENT")
    void financialItemsAreCountedAsUnread() throws Exception {
        JsonNode data = notifications(myUserId);

        assertTrue(data.path("unreadCount").asLong() >= 2,
                "the payment and its receipt are both unread, so unreadCount must reflect them; got "
                        + data.path("unreadCount").asLong());
    }

    @Test
    @WithMockUser(roles = "PARENT")
    void theParentSeesTheirOwnLearnerNotificationsAlongsideTheMoney() throws Exception {
        // Wiring the assembler in must not displace what the endpoint already served.
        JsonNode data = notifications(myUserId);
        assertTrue(data.path("notifications").size() >= 2,
                "the merged response must still carry items; got " + ids(data));
    }

    // ------------------------------------------------------------------ who may see whose money

    @Test
    @WithMockUser(roles = "PARENT")
    void anotherFamilysPaymentIsNeverInTheResponse() throws Exception {
        JsonNode data = notifications(myUserId);

        assertFalse(containsId(data, "payment-" + theirPayment.getId()),
                "another family's payment must never appear; got: " + ids(data));
        assertFalse(containsId(data, "receipt-" + theirPayment.getId()),
                "another family's receipt must never appear; got: " + ids(data));
    }

    @Test
    @WithMockUser(roles = "PARENT")
    void aPaymentForAChildThisParentHasNotLinkedIsNotShown() throws Exception {
        // The payment names me as the payer, but the child is not one of my verified links. The
        // link is what authorises the relationship; paying alone does not create one.
        JsonNode data = notifications(myUserId);

        assertFalse(containsId(data, "payment-" + unlinkedChildPayment.getId()),
                "a payment for an unlinked child must be withheld; got: " + ids(data));
    }

    @Test
    @WithMockUser(roles = "PARENT")
    void moneyIsScopedToAPayerIdNotToAnyAuthenticatedCaller() throws Exception {
        // Parent B asks. They have a linked child and a completed payment of their own, so an
        // empty answer would be wrong; the point is which payment they get.
        JsonNode data = notifications(otherParentUserId());

        assertTrue(containsId(data, "payment-" + theirPayment.getId()),
                "a payer must see their own payment; got: " + ids(data));
        assertFalse(containsId(data, "payment-" + myPayment.getId()),
                "a payer must never see mine; got: " + ids(data));
    }

    // ------------------------------------------------------------------ the parent's own preference

    @Test
    @WithMockUser(roles = "PARENT")
    void turningFeeAlertsOffRemovesEveryFinancialItem() throws Exception {
        preferenceRepository.save(ParentNotificationPreference.builder()
                .parentId(me.getId())
                .feeAlerts(false)
                .build());

        JsonNode data = notifications(myUserId);

        assertFalse(containsType(data, "PAYMENT"), "fee alerts off must suppress payment items");
        assertFalse(containsType(data, "RECEIPT"), "fee alerts off must suppress receipt items");
    }

    @Test
    @WithMockUser(roles = "PARENT")
    void anAbsentPreferenceStillShowsMoneyRatherThanHidingIt() throws Exception {
        // Never having touched the settings must not read as "do not tell me about my money".
        JsonNode data = notifications(myUserId);

        assertTrue(containsId(data, "payment-" + myPayment.getId()),
                "an absent preference falls back to showing alerts; got: " + ids(data));
    }

    @Test
    @WithMockUser(roles = "PARENT")
    void aCallerWithNoParentProfileGetsAnEmptyInboxRatherThanAnError() throws Exception {
        UUID orphan = newUser(User.Role.PARENT);

        JsonNode data = notifications(orphan);

        assertFalse(containsType(data, "PAYMENT"),
                "no parent profile means no parent financial records, not a failure");
    }

    // ------------------------------------------------------------------ helpers

    private JsonNode notifications(UUID userId) throws Exception {
        MvcResult result = mockMvc.perform(get("/v1/my/notifications")
                        .requestAttr("userId", userId)
                        .param("page", "0")
                        .param("size", "200"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        assertTrue(data.path("notifications").isArray(), "the response must carry a notifications array");
        return data;
    }

    private static boolean containsId(JsonNode data, String id) {
        for (JsonNode n : data.path("notifications")) {
            if (id.equals(n.path("id").asText())) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsType(JsonNode data, String type) {
        for (JsonNode n : data.path("notifications")) {
            if (type.equals(n.path("type").asText())) {
                return true;
            }
        }
        return false;
    }

    private static List<String> ids(JsonNode data) {
        List<String> out = new ArrayList<>();
        data.path("notifications").forEach(n -> out.add(n.path("id").asText()));
        return out;
    }

    private UUID otherParentUserId() {
        return otherParent.getUserId();
    }

    private Payment payment(UUID parentId, UUID studentId, String receipt) {
        Payment p = Payment.builder()
                .parentId(parentId)
                .studentId(studentId)
                .serviceType("MONTHLY_ACCESS")
                .amount(new BigDecimal("5000"))
                .currency("TZS")
                .status("COMPLETED")
                .receiptNumber(receipt)
                .paidAt(LocalDateTime.now().minusDays(1))
                .build();
        p.setInstitutionId(institutionId);
        return paymentRepository.saveAndFlush(p);
    }

    private UUID newUser(User.Role role) {
        User saved = userRepository.save(User.builder()
                .email(role.name().toLowerCase() + "-" + UUID.randomUUID() + "@test.go.tz")
                .firstName("Test")
                .lastName("User")
                .passwordHash("x")
                .role(role)
                .isActive(true)
                .build());
        createdUsers.add(saved.getId());
        return saved.getId();
    }

    private Student newStudent() {
        return studentRepository.save(withInstitution(Student.builder()
                .userId(newUser(User.Role.STUDENT))
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

    @Test
    void theFixturesAreRealSoTheIsolationAssertionsMeanSomething() {
        // Checked by id rather than by count: the test database is shared with other suites, so a
        // global count would not be three. What matters is that all three of ours exist - without
        // them, "not present in the response" would only prove there was nothing to find.
        assertTrue(paymentRepository.existsById(myPayment.getId()), "my payment must exist");
        assertTrue(paymentRepository.existsById(theirPayment.getId()), "the other payment must exist");
        assertTrue(paymentRepository.existsById(unlinkedChildPayment.getId()),
                "the unlinked-child payment must exist");
    }
}
