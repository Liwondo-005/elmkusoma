package tz.elmkusoma.assessment.controller;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tz.elmkusoma.assessment.domain.Assessment;
import tz.elmkusoma.assessment.domain.Option;
import tz.elmkusoma.assessment.domain.Question;
import tz.elmkusoma.assessment.domain.QuestionType;
import tz.elmkusoma.assessment.repository.*;
import tz.elmkusoma.assessment.service.impl.AssessmentServiceImpl;
import tz.elmkusoma.audit.service.AuditService;
import tz.elmkusoma.common.ClassAccessGuard;
import tz.elmkusoma.exception.GlobalExceptionHandler;
import tz.elmkusoma.learner.service.NotificationService;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.student.repository.StudentRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * B22: the answer key never reaches a non-teacher caller over HTTP — the
 * controller derives {@code includeAnswerKey} from the JWT role and the service
 * strips {@code option.isCorrect} before the response is serialized. Uses
 * standalone MockMvc with the real service (no Spring context, no DB).
 */
@ExtendWith(MockitoExtension.class)
class AssessmentControllerAnswerKeyTest {

    private static final UUID ASSESSMENT_ID = UUID.randomUUID();
    private static final UUID INSTITUTION_ID = UUID.randomUUID();
    private static final UUID QUESTION_ID = UUID.randomUUID();
    private static final UUID OPTION_ID = UUID.randomUUID();

    @Mock private AssessmentRepository assessmentRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private OptionRepository optionRepository;
    @Mock private AttemptRepository attemptRepository;
    @Mock private AnswerRepository answerRepository;
    @Mock private AssessmentResultRepository resultRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationService notificationService;
    @Mock private AuditService auditService;
    @Mock private ClassAccessGuard classAccessGuard;

    @InjectMocks
    private AssessmentServiceImpl service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // Mirrors Boot's auto-configured mapper: finds JavaTimeModule (etc.) so
        // LocalDateTime fields in response DTOs serialize exactly as in the app.
        ObjectMapper mapper = Jackson2ObjectMapperBuilder.json().build();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mockMvc = MockMvcBuilders.standaloneSetup(new AssessmentController(service))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(mapper))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private void givenPublishedAssessmentWithKey() {
        when(assessmentRepository.findById(ASSESSMENT_ID)).thenReturn(Optional.of(Assessment.builder()
                .id(ASSESSMENT_ID)
                .institutionId(INSTITUTION_ID)
                .subjectId(UUID.randomUUID())
                .classGroupId(UUID.randomUUID())
                .title("Algebra quiz")
                .totalMarks(50)
                .passMarks(30)
                .isPublished(true)
                .status("PUBLISHED")
                .isDeleted(false)
                .build()));
        when(questionRepository.findByAssessmentIdAndIsDeletedFalseOrderBySortOrder(ASSESSMENT_ID))
                .thenReturn(List.of(Question.builder()
                        .id(QUESTION_ID)
                        .assessmentId(ASSESSMENT_ID)
                        .questionType(QuestionType.MCQ)
                        .questionText("2+2?")
                        .marks(4)
                        .sortOrder(0)
                        .isDeleted(false)
                        .build()));
        when(optionRepository.findByQuestionIdAndIsDeletedFalseOrderBySortOrder(QUESTION_ID))
                .thenReturn(List.of(Option.builder()
                        .id(OPTION_ID)
                        .questionId(QUESTION_ID)
                        .optionText("4")
                        .isCorrect(true)
                        .sortOrder(0)
                        .isDeleted(false)
                        .build()));
    }

    @Test
    void studentGetsQuestionsWithoutAnswerKey() throws Exception {
        givenPublishedAssessmentWithKey();

        mockMvc.perform(get("/v1/assessments/{id}/questions", ASSESSMENT_ID)
                        .header("X-Institution-Id", INSTITUTION_ID)
                        .requestAttr("userRole", "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].options[0].optionText").value("4"))
                .andExpect(jsonPath("$.data[0].options[0].isCorrect").doesNotExist());
    }

    @Test
    void parentGetsQuestionsWithoutAnswerKey() throws Exception {
        givenPublishedAssessmentWithKey();

        mockMvc.perform(get("/v1/assessments/{id}/questions", ASSESSMENT_ID)
                        .header("X-Institution-Id", INSTITUTION_ID)
                        .requestAttr("userRole", "PARENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].options[0].optionText").value("4"))
                .andExpect(jsonPath("$.data[0].options[0].isCorrect").doesNotExist());
    }

    @Test
    void teacherGetsQuestionsWithAnswerKey() throws Exception {
        givenPublishedAssessmentWithKey();

        mockMvc.perform(get("/v1/assessments/{id}/questions", ASSESSMENT_ID)
                        .header("X-Institution-Id", INSTITUTION_ID)
                        .requestAttr("userRole", "TEACHER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].options[0].isCorrect").value(true));
    }

    @Test
    void institutionAdminGetsQuestionsWithAnswerKey() throws Exception {
        givenPublishedAssessmentWithKey();

        mockMvc.perform(get("/v1/assessments/{id}/questions", ASSESSMENT_ID)
                        .header("X-Institution-Id", INSTITUTION_ID)
                        .requestAttr("userRole", "INSTITUTION_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].options[0].isCorrect").value(true));
    }

    @Test
    void platformAdminGetsQuestionsWithAnswerKey() throws Exception {
        givenPublishedAssessmentWithKey();

        mockMvc.perform(get("/v1/assessments/{id}/questions", ASSESSMENT_ID)
                        .header("X-Institution-Id", INSTITUTION_ID)
                        .requestAttr("userRole", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].options[0].isCorrect").value(true));
    }

    @Test
    void studentNeverSeesQuestionsOfUnpublishedAssessment() throws Exception {
        when(assessmentRepository.findById(ASSESSMENT_ID)).thenReturn(Optional.of(Assessment.builder()
                .id(ASSESSMENT_ID)
                .institutionId(INSTITUTION_ID)
                .subjectId(UUID.randomUUID())
                .classGroupId(UUID.randomUUID())
                .title("Algebra quiz")
                .totalMarks(50)
                .passMarks(30)
                .isPublished(false)
                .status("DRAFT")
                .isDeleted(false)
                .build()));

        mockMvc.perform(get("/v1/assessments/{id}/questions", ASSESSMENT_ID)
                        .header("X-Institution-Id", INSTITUTION_ID)
                        .requestAttr("userRole", "STUDENT"))
                .andExpect(status().isNotFound());
    }
}
