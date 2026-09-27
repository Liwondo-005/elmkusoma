package tz.elmkusoma.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import tz.elmkusoma.testutil.TestDataSeeder;
import tz.elmkusoma.testutil.TestTokens;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class CrossOrgAttackTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private tz.elmkusoma.certificate.repository.CertificateRepository certificateRepository;

    @Test
    void eventIdManipulation_CrossInstitution_Returns403() throws Exception {
        String adminToken = TestTokens.adminToken();
        String otherStudentToken = TestTokens.otherStudentToken();

        // Admin creates event in institution A
        String eventId = "00000000-0000-0000-0000-000000000099";

        // Other student from institution B tries to access
        mockMvc.perform(get("/v1/events/" + eventId)
                .header("Authorization", "Bearer " + otherStudentToken))
            .andExpect(status().isForbidden());
    }

    @Test
    void institutionIdManipulation_CrossOrg_Returns403() throws Exception {
        String adminToken = TestTokens.adminToken();

        // Try to access another institution's data
        mockMvc.perform(get("/v1/admin/people")
                .header("Authorization", "Bearer " + adminToken)
                .header("X-Institution-Id", "11111111-1111-1111-1111-111111111111"))
            .andExpect(status().isForbidden());
    }

    @Test
    void courseIdManipulation_CrossInstitution_Returns403() throws Exception {
        String studentToken = TestTokens.studentToken();

        mockMvc.perform(get("/v1/courses/00000000-0000-0000-0000-000000000088")
                .header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isForbidden());
    }

    @Test
    void paymentIdManipulation_CrossInstitution_Returns403() throws Exception {
        String adminToken = TestTokens.adminToken();

        mockMvc.perform(get("/v1/payments/00000000-0000-0000-0000-000000000077")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isForbidden());
    }

    @Test
    void certificateIdManipulation_CrossInstitution_Returns403() throws Exception {
        String adminToken = TestTokens.adminToken();

        // /v1/certificates/verify/** is intentionally public (permitAll), so the
        // tenant-isolation check lives on the authenticated detail endpoint:
        // seed a certificate that belongs to a DIFFERENT institution than the
        // admin token's (…0001) and expect 403 on /v1/certificates/{id}.
        tz.elmkusoma.certificate.domain.Certificate foreign =
                tz.elmkusoma.certificate.domain.Certificate.builder()
                        .templateId(java.util.UUID.randomUUID())
                        .studentId(java.util.UUID.randomUUID())
                        .issuedBy(java.util.UUID.randomUUID())
                        .serialNumber("XORG-TEST-066")
                        .certificateType(tz.elmkusoma.certificate.domain.Certificate.CertificateType.COMPLETION)
                        .title("Cross-org certificate")
                        .studentName("Other Institution Student")
                        .completionDate(java.time.LocalDate.now())
                        .issueDate(java.time.LocalDateTime.now())
                        .verificationCode("xorg-verify-066")
                        .status(tz.elmkusoma.certificate.domain.Certificate.CertificateStatus.ISSUED)
                        .build();
        foreign.setInstitutionId(java.util.UUID.fromString("00000000-0000-0000-0000-000000000002"));
        foreign = certificateRepository.save(foreign);

        mockMvc.perform(get("/v1/certificates/" + foreign.getId())
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isForbidden());
    }

    @Test
    void fileIdManipulation_CrossInstitution_Returns403() throws Exception {
        String studentToken = TestTokens.studentToken();

        mockMvc.perform(get("/v1/media/00000000-0000-0000-0000-000000000055")
                .header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isForbidden());
    }

    @Test
    void exportIdManipulation_CrossInstitution_Returns403() throws Exception {
        String adminToken = TestTokens.adminToken();

        mockMvc.perform(get("/v1/admin/export")
                .header("Authorization", "Bearer " + adminToken)
                .param("entityType", "users")
                .header("X-Institution-Id", "22222222-2222-2222-2222-222222222222"))
            .andExpect(status().isForbidden());
    }
}