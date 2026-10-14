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
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    // Injected into the controller for the download endpoint. A learner who cannot open a
    // certificate must not be able to download it: a file that has left the system cannot be
    // un-served the way a page can stop being served.
    @Mock private tz.elmkusoma.certificate.pdf.CertificatePdfService certificatePdfService;

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
    @DisplayName("a student without a studentId parameter is refused the institution-wide list")
    void aStudentCannotListTheWholeInstitution() {
        // Omitting studentId selects the unfiltered branch, which returns every certificate in the
        // institution: other students' names, serials, grades and completion dates. The endpoint
        // admits STUDENT, but the ownership checks only ever ran when studentId was present, so
        // the learner-facing call /v1/certificates (no params) returned the whole school.
        ResponseEntity<ApiResponse<org.springframework.data.domain.Page<CertificateResponse>>> response =
                controller.getCertificates(null, institutionId, userId, "STUDENT", 0, 20);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode(),
                "an unnamed student is an institution-wide query and must not be served to a learner");
    }

    @Test
    @DisplayName("a parent without a studentId parameter is refused the institution-wide list")
    void aParentCannotListTheWholeInstitution() {
        ResponseEntity<ApiResponse<org.springframework.data.domain.Page<CertificateResponse>>> response =
                controller.getCertificates(null, institutionId, userId, "PARENT", 0, 20);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode(),
                "a parent must name a linked child, not receive every certificate in the school");
    }

    @Test
    @DisplayName("an administrator keeps the institution-wide list")
    void anAdministratorKeepsTheInstitutionWideList() {
        when(certificateService.getCertificatesByInstitution(eq(institutionId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(java.util.List.of()));

        ResponseEntity<ApiResponse<org.springframework.data.domain.Page<CertificateResponse>>> response =
                controller.getCertificates(null, institutionId, userId, "ADMIN", 0, 20);

        assertEquals(HttpStatus.OK, response.getStatusCode(),
                "the unfiltered branch exists for staff, and is still reachable by staff");
    }

    @Test
    @DisplayName("a linked parent can read their child's certificate")    void aLinkedParentCanReadTheChildsCertificate() {
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

    // ---- Download ----

    @Test
    @DisplayName("a student cannot download a classmate's certificate")
    void aStudentCannotDownloadAnotherStudentsCertificate() {
        UUID certificateId = UUID.randomUUID();
        UUID someoneElseRowId = UUID.randomUUID();
        when(studentRepository.findByUserIdAndIsDeletedFalse(userId))
                .thenReturn(Optional.of(studentRowFor(studentRowId, userId)));
        when(certificateService.getCertificateById(certificateId, institutionId))
                .thenReturn(certificateOwnedBy(someoneElseRowId));

        // Thrown rather than returned as a body: the success path has type byte[], so there is no
        // place for an ApiResponse in this response and GlobalExceptionHandler maps it to 403.
        assertThrows(tz.elmkusoma.exception.ForbiddenException.class,
                () -> controller.exportCertificatePdf(certificateId, institutionId, userId, "STUDENT"));
    }

    @Test
    @DisplayName("a student downloads their own certificate as a PDF")
    void aStudentCanDownloadTheirOwnCertificate() {
        UUID certificateId = UUID.randomUUID();
        when(studentRepository.findByUserIdAndIsDeletedFalse(userId))
                .thenReturn(Optional.of(studentRowFor(studentRowId, userId)));
        when(certificateService.getCertificateById(certificateId, institutionId))
                .thenReturn(certificateOwnedBy(studentRowId));
        when(certificatePdfService.exportCertificate(certificateId, institutionId))
                .thenReturn(new byte[]{'%', 'P', 'D', 'F'});

        ResponseEntity<byte[]> response =
                controller.exportCertificatePdf(certificateId, institutionId, userId, "STUDENT");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("application/pdf", response.getHeaders().getContentType().toString());
        assertEquals("attachment; filename=\"certificate-CERT-OWN-001.pdf\"",
                response.getHeaders().getFirst("Content-Disposition"),
                "the serial goes into the filename, and a serial is operator-visible input");
    }

    @Test
    @DisplayName("an unlinked parent cannot download either")
    void anUnlinkedParentCannotDownload() {
        UUID certificateId = UUID.randomUUID();
        when(parentRepository.findByUserIdAndIsDeletedFalse(userId)).thenReturn(Optional.empty());
        when(certificateService.getCertificateById(certificateId, institutionId))
                .thenReturn(certificateOwnedBy(studentRowId));

        assertThrows(tz.elmkusoma.exception.ForbiddenException.class,
                () -> controller.exportCertificatePdf(certificateId, institutionId, userId, "PARENT"));
    }

    @Test
    @DisplayName("staff are not ownership-checked, only tenant-checked by the service")
    void staffCanDownload() {
        UUID certificateId = UUID.randomUUID();
        when(certificateService.getCertificateById(certificateId, institutionId))
                .thenReturn(certificateOwnedBy(studentRowId));
        when(certificatePdfService.exportCertificate(certificateId, institutionId))
                .thenReturn(new byte[]{'%', 'P', 'D', 'F'});

        ResponseEntity<byte[]> response =
                controller.exportCertificatePdf(certificateId, institutionId, userId, "INSTITUTION_ADMIN");

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @DisplayName("a serial that could break the header is sanitised out of the filename")
    void theFilenameCannotEscapeTheHeader() {
        UUID certificateId = UUID.randomUUID();
        CertificateResponse response = certificateOwnedBy(studentRowId);
        response.setSerialNumber("CERT-\"-INJECTED");
        when(studentRepository.findByUserIdAndIsDeletedFalse(userId))
                .thenReturn(Optional.of(studentRowFor(studentRowId, userId)));
        when(certificateService.getCertificateById(certificateId, institutionId)).thenReturn(response);
        when(certificatePdfService.exportCertificate(certificateId, institutionId))
                .thenReturn(new byte[]{'%', 'P', 'D', 'F'});

        ResponseEntity<byte[]> exported =
                controller.exportCertificatePdf(certificateId, institutionId, userId, "STUDENT");

        String disposition = exported.getHeaders().getFirst("Content-Disposition");
        assertEquals("attachment; filename=\"certificate-CERT---INJECTED.pdf\"", disposition,
                "the quote that would end the header early is replaced; the surrounding dashes are "
                        + "already legal filename characters and are left alone");
        assertEquals(1, disposition.chars().filter(c -> c == '"').count() / 2,
                "the header must contain exactly one quoted filename");
    }
}
