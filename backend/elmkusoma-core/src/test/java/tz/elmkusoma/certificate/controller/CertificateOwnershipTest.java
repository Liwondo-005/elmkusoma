package tz.elmkusoma.certificate.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.certificate.dto.CertificateResponse;
import tz.elmkusoma.certificate.service.CertificateApprovalService;
import tz.elmkusoma.certificate.service.CertificateService;
import tz.elmkusoma.parent.domain.Parent;
import tz.elmkusoma.parent.repository.ParentRepository;
import tz.elmkusoma.parent.repository.ParentStudentLinkRepository;
import tz.elmkusoma.student.domain.Student;
import tz.elmkusoma.student.repository.StudentRepository;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Certificate ownership across the two identifier spaces the system actually uses.
 *
 * <p>{@code certificates.student_id} is a students.id. The authenticated principal is a users.id.
 * They are different values in different namespaces, so comparing them directly is a category
 * error: it denied every student access to their own certificates while looking, in tests, like a
 * reasonable equality check.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CertificateOwnershipTest {

    @Mock private CertificateService certificateService;
    @Mock private CertificateApprovalService certificateApprovalService;
    @Mock private StudentRepository studentRepository;
    @Mock private ParentRepository parentRepository;
    @Mock private ParentStudentLinkRepository parentStudentLinkRepository;

    @InjectMocks private CertificateController controller;

    private UUID institutionId;
    private UUID userId;
    private UUID studentRowId;

    @BeforeEach
    void setUp() {
        institutionId = UUID.randomUUID();
        userId = UUID.randomUUID();
        studentRowId = UUID.randomUUID();
    }

    private Student studentRowFor(UUID rowId, UUID linkedUserId) {
        Student student = new Student();
        student.setId(rowId);
        student.setUserId(linkedUserId);
        student.setInstitutionId(institutionId);
        return student;
    }

    private CertificateResponse certificateOwnedBy(UUID ownerStudentRowId) {
        CertificateResponse response = new CertificateResponse();
        response.setId(UUID.randomUUID());
        response.setSerialNumber("CERT-OWN-001");
        response.setStudentId(ownerStudentRowId);
        response.setStudentName("Amini Mwakalinga");
        response.setStatus("ISSUED");
        return response;
    }

    @Test
    @DisplayName("a student can open their own certificate")
    void aStudentCanReadTheirOwnCertificate() {
        UUID certificateId = UUID.randomUUID();
        when(studentRepository.findByUserIdAndIsDeletedFalse(userId))
                .thenReturn(Optional.of(studentRowFor(studentRowId, userId)));
        when(certificateService.getCertificateById(certificateId, institutionId))
                .thenReturn(certificateOwnedBy(studentRowId));

        ResponseEntity<ApiResponse<CertificateResponse>> response =
                controller.getCertificateById(certificateId, institutionId, userId, "STUDENT");

        assertEquals(HttpStatus.OK, response.getStatusCode(),
                "the certificate belongs to this student's student row, so access must be granted");
    }

    @Test
    @DisplayName("a student cannot open a classmate's certificate")
    void aStudentCannotReadAnotherStudentsCertificate() {
        UUID certificateId = UUID.randomUUID();
        UUID someoneElseRowId = UUID.randomUUID();
        when(studentRepository.findByUserIdAndIsDeletedFalse(userId))
                .thenReturn(Optional.of(studentRowFor(studentRowId, userId)));
        when(certificateService.getCertificateById(certificateId, institutionId))
                .thenReturn(certificateOwnedBy(someoneElseRowId));

        ResponseEntity<ApiResponse<CertificateResponse>> response =
                controller.getCertificateById(certificateId, institutionId, userId, "STUDENT");

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    @DisplayName("a student with no student record cannot read a certificate")
    void aStudentWithoutAStudentRowIsRefused() {
        UUID certificateId = UUID.randomUUID();
        when(studentRepository.findByUserIdAndIsDeletedFalse(userId)).thenReturn(Optional.empty());
        when(certificateService.getCertificateById(certificateId, institutionId))
                .thenReturn(certificateOwnedBy(studentRowId));

        ResponseEntity<ApiResponse<CertificateResponse>> response =
                controller.getCertificateById(certificateId, institutionId, userId, "STUDENT");

        // Fails closed rather than granting access when the caller cannot be resolved.
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    @DisplayName("a student can list their own certificates")
    void aStudentCanListTheirOwnCertificates() {
        when(studentRepository.findByUserIdAndIsDeletedFalse(userId))
                .thenReturn(Optional.of(studentRowFor(studentRowId, userId)));
        when(certificateService.getCertificatesByStudent(eq(studentRowId), eq(institutionId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(java.util.List.of()));

        ResponseEntity<ApiResponse<org.springframework.data.domain.Page<CertificateResponse>>> response =
                controller.getCertificates(studentRowId, institutionId, userId, "STUDENT", 0, 20);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @DisplayName("a student cannot list another student's certificates")
    void aStudentCannotListAnotherStudentsCertificates() {
        when(studentRepository.findByUserIdAndIsDeletedFalse(userId))
                .thenReturn(Optional.of(studentRowFor(studentRowId, userId)));

        ResponseEntity<ApiResponse<org.springframework.data.domain.Page<CertificateResponse>>> response =
                controller.getCertificates(UUID.randomUUID(), institutionId, userId, "STUDENT", 0, 20);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    @DisplayName("a linked parent can read their child's certificate")
    void aLinkedParentCanReadTheChildsCertificate() {
        UUID certificateId = UUID.randomUUID();
        UUID parentRowId = UUID.randomUUID();

        Parent parent = new Parent();
        parent.setId(parentRowId);
        parent.setUserId(userId);
        when(parentRepository.findByUserIdAndIsDeletedFalse(userId)).thenReturn(Optional.of(parent));
        when(parentStudentLinkRepository.existsByParentIdAndStudentIdAndIsDeletedFalse(parentRowId, studentRowId))
                .thenReturn(true);
        when(certificateService.getCertificateById(certificateId, institutionId))
                .thenReturn(certificateOwnedBy(studentRowId));

        ResponseEntity<ApiResponse<CertificateResponse>> response =
                controller.getCertificateById(certificateId, institutionId, userId, "PARENT");

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @DisplayName("an unlinked parent is refused")
    void anUnlinkedParentIsRefused() {
        UUID certificateId = UUID.randomUUID();
        UUID parentRowId = UUID.randomUUID();

        Parent parent = new Parent();
        parent.setId(parentRowId);
        parent.setUserId(userId);
        when(parentRepository.findByUserIdAndIsDeletedFalse(userId)).thenReturn(Optional.of(parent));
        when(parentStudentLinkRepository.existsByParentIdAndStudentIdAndIsDeletedFalse(any(), any()))
                .thenReturn(false);
        when(certificateService.getCertificateById(certificateId, institutionId))
                .thenReturn(certificateOwnedBy(studentRowId));

        ResponseEntity<ApiResponse<CertificateResponse>> response =
                controller.getCertificateById(certificateId, institutionId, userId, "PARENT");

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }
}
