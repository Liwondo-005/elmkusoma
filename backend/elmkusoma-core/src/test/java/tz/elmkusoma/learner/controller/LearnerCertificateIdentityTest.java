package tz.elmkusoma.learner.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import tz.elmkusoma.certificate.domain.Certificate;
import tz.elmkusoma.certificate.repository.CertificateRepository;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.course.repository.*;
import tz.elmkusoma.learning.repository.LessonProgressRepository;
import tz.elmkusoma.learning.repository.LessonRepository;
import tz.elmkusoma.learning.repository.ResourceRepository;
import tz.elmkusoma.learning.repository.VideoTutorialRepository;
import tz.elmkusoma.learning.service.ResourceService;
import tz.elmkusoma.learner.repository.*;
import tz.elmkusoma.liveclass.repository.LiveClassParticipantRepository;
import tz.elmkusoma.shared.repository.InstitutionRepository;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.student.domain.Student;
import tz.elmkusoma.student.repository.StudentRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * The learner's own certificate view resolves users.id to a students.id before querying.
 *
 * <p>{@code certificates.student_id} holds a students.id, while the authenticated principal is a
 * users.id. Querying with the principal directly compares two unrelated identifier spaces, so the
 * learner's certificate page returned nothing even for a student who held issued certificates.
 * The lesson is the same one the certificate controller already learned: resolve first, then
 * compare inside one namespace.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LearnerCertificateIdentityTest {

    @Mock private GeneralLearnerProfileRepository profileRepository;
    @Mock private BookmarkRepository bookmarkRepository;
    @Mock private LearnerNotificationRepository notificationRepository;
    @Mock private LearnerEnrollmentRepository enrollmentRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private CourseModuleRepository courseModuleRepository;
    @Mock private CourseLessonRepository courseLessonRepository;
    @Mock private ResourceRepository resourceRepository;
    @Mock private ResourceService resourceService;
    @Mock private VideoTutorialRepository videoTutorialRepository;
    @Mock private LessonRepository lessonRepository;
    @Mock private LiveClassRepository liveClassRepository;
    @Mock private AnnouncementRepository announcementRepository;
    @Mock private LessonProgressRepository lessonProgressRepository;
    @Mock private CertificateRepository certificateRepository;
    @Mock private tz.elmkusoma.certificate.repository.CertificateTemplateRepository certificateTemplateRepository;
    @Mock private UserRepository userRepository;
    @Mock private InstitutionRepository institutionRepository;
    @Mock private LiveClassParticipantRepository liveClassParticipantRepository;
    @Mock private tz.elmkusoma.teacher.repository.TeacherRepository teacherRepository;
    @Mock private tz.elmkusoma.academic.repository.SubjectRepository subjectRepository;
    @Mock private LearnerGoalRepository learningGoalRepository;
    @Mock private StudentRepository studentRepository;

    @InjectMocks private LearnerController controller;

    private UUID userId;
    private UUID studentRowId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        studentRowId = UUID.randomUUID();
    }

    private Student studentRow() {
        Student student = new Student();
        student.setId(studentRowId);
        student.setUserId(userId);
        return student;
    }

    private Certificate issuedCertificate(UUID ownerStudentRowId) {
        Certificate certificate = new Certificate();
        certificate.setId(UUID.randomUUID());
        certificate.setStudentId(ownerStudentRowId);
        certificate.setTitle("Certificate of Completion");
        certificate.setStatus(Certificate.CertificateStatus.ISSUED);
        certificate.setSerialNumber("CERT-ID-001");
        certificate.setCompletionDate(LocalDate.of(2026, 3, 1));
        certificate.setIssueDate(LocalDateTime.of(2026, 3, 1, 9, 0));
        certificate.setIsDeleted(false);
        return certificate;
    }

    @Test
    @DisplayName("a learner's issued certificates are found through their student record")
    void aLearnerSeesTheirOwnIssuedCertificates() {
        when(studentRepository.findByUserIdAndIsDeletedFalse(userId))
                .thenReturn(Optional.of(studentRow()));
        when(certificateRepository.findIssuedByStudentId(studentRowId))
                .thenReturn(List.of(issuedCertificate(studentRowId)));

        ResponseEntity<ApiResponse<List<Certificate>>> response = controller.myCertificates(userId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().getData().size(),
                "the query must be keyed on the student row, not the user id");
        verify(certificateRepository).findIssuedByStudentId(studentRowId);
    }

    @Test
    @DisplayName("a caller with no student record gets an empty list rather than an error")
    void aCallerWithoutAStudentRowGetsNothing() {
        when(studentRepository.findByUserIdAndIsDeletedFalse(userId)).thenReturn(Optional.empty());

        ResponseEntity<ApiResponse<List<Certificate>>> response = controller.myCertificates(userId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().getData().isEmpty(),
                "teachers have no student row, so there is nothing to resolve and nothing to leak");
        verifyNoInteractions(certificateRepository);
    }

    @Test
    @DisplayName("a certificate detail resolves the student before comparing ownership")
    void aCertificateDetailResolvesTheStudentFirst() {
        UUID certificateId = UUID.randomUUID();
        Certificate certificate = issuedCertificate(studentRowId);
        certificate.setId(certificateId);

        when(studentRepository.findByUserIdAndIsDeletedFalse(userId))
                .thenReturn(Optional.of(studentRow()));
        when(certificateRepository.findById(certificateId)).thenReturn(Optional.of(certificate));

        ResponseEntity<ApiResponse<Certificate>> response = controller.myCertificateDetail(userId, certificateId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @DisplayName("another student's certificate detail is not found")
    void anotherStudentsCertificateDetailIsNotFound() {
        UUID certificateId = UUID.randomUUID();
        Certificate certificate = issuedCertificate(UUID.randomUUID());
        certificate.setId(certificateId);

        when(studentRepository.findByUserIdAndIsDeletedFalse(userId))
                .thenReturn(Optional.of(studentRow()));
        when(certificateRepository.findById(certificateId)).thenReturn(Optional.of(certificate));

        ResponseEntity<ApiResponse<Certificate>> response = controller.myCertificateDetail(userId, certificateId);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    @DisplayName("a detail request without a student record is not found")
    void aDetailRequestWithoutAStudentRowIsNotFound() {
        UUID certificateId = UUID.randomUUID();
        when(studentRepository.findByUserIdAndIsDeletedFalse(userId)).thenReturn(Optional.empty());

        ResponseEntity<ApiResponse<Certificate>> response = controller.myCertificateDetail(userId, certificateId);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode(),
                "fails closed: no resolvable student means no certificate is claimable");
        verifyNoInteractions(certificateRepository);
    }
}
