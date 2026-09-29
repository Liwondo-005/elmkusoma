package tz.elmkusoma.grading.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.academic.domain.ClassGroup;
import tz.elmkusoma.academic.repository.ClassGroupRepository;
import tz.elmkusoma.assessment.domain.Assessment;
import tz.elmkusoma.assessment.domain.AssessmentResult;
import tz.elmkusoma.assessment.repository.AssessmentRepository;
import tz.elmkusoma.assessment.repository.AssessmentResultRepository;
import tz.elmkusoma.common.ClassAccessGuard;
import tz.elmkusoma.grading.dto.response.GradebookResponse;
import tz.elmkusoma.grading.service.GradebookService;
import tz.elmkusoma.learning.domain.Assignment;
import tz.elmkusoma.learning.domain.AssignmentSubmission;
import tz.elmkusoma.learning.repository.AssignmentRepository;
import tz.elmkusoma.learning.repository.AssignmentSubmissionRepository;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.student.domain.Student;
import tz.elmkusoma.student.repository.StudentRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GradebookServiceImpl implements GradebookService {

    private final ClassAccessGuard classAccessGuard;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository submissionRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentResultRepository resultRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final ClassGroupRepository classGroupRepository;

    @Override
    public GradebookResponse getGradebook(UUID classGroupId, UUID institutionId) {
        List<UUID> studentIds = classAccessGuard.resolveClassStudentIds(classGroupId);

        String className = null;
        ClassGroup classGroup = classGroupRepository.findById(classGroupId).orElse(null);
        if (classGroup != null) {
            className = classGroup.getName();
        }

        List<Assignment> assignments = assignmentRepository.findByClassGroupIdAndIsDeletedFalse(classGroupId)
                .stream()
                .filter(a -> institutionId == null || institutionId.equals(a.getInstitutionId()))
                .sorted(Comparator.comparing(Assignment::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        List<Assessment> assessments = assessmentRepository.findByClassGroupIdAndIsDeletedFalse(classGroupId)
                .stream()
                .filter(a -> institutionId == null || institutionId.equals(a.getInstitutionId()))
                .sorted(Comparator.comparing(Assessment::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        // One query each for every student's submissions/results in this class.
        Map<String, AssignmentSubmission> submissionsByKey = new HashMap<>();
        if (!studentIds.isEmpty()) {
            for (AssignmentSubmission submission
                    : submissionRepository.findByStudentIdInAndIsDeletedFalse(studentIds)) {
                submissionsByKey.put(submission.getStudentId() + ":" + submission.getAssignmentId(), submission);
            }
        }
        Map<String, AssessmentResult> resultsByKey = new HashMap<>();
        if (!studentIds.isEmpty()) {
            for (AssessmentResult result : resultRepository.findByStudentIdInAndIsDeletedFalse(studentIds)) {
                resultsByKey.put(result.getStudentId() + ":" + result.getAssessmentId(), result);
            }
        }

        // Learner display names in one query.
        List<Student> students = studentIds.stream()
                .map(id -> studentRepository.findById(id).orElse(null))
                .filter(s -> s != null)
                .toList();
        Map<UUID, User> usersById = students.isEmpty() ? Map.of()
                : userRepository.findAllById(
                        students.stream()
                                .map(Student::getUserId)
                                .filter(java.util.Objects::nonNull)
                                .collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(User::getId, Function.identity()));

        List<GradebookResponse.Row> rows = new ArrayList<>();
        for (Student student : students) {
            List<GradebookResponse.Item> assignmentItems = new ArrayList<>();
            int obtained = 0;
            int obtainable = 0;

            for (Assignment assignment : assignments) {
                AssignmentSubmission submission =
                        submissionsByKey.get(student.getId() + ":" + assignment.getId());
                String status;
                Integer score = null;
                if (submission == null) {
                    status = "NOT_SUBMITTED";
                } else if (Boolean.TRUE.equals(submission.getIsDraft())) {
                    status = "DRAFT";
                } else if (submission.getGrade() != null) {
                    status = "GRADED";
                    score = submission.getGrade();
                } else {
                    status = "SUBMITTED";
                }
                if (score != null) {
                    obtained += score;
                    obtainable += assignment.getTotalMarks() != null ? assignment.getTotalMarks() : 0;
                }
                assignmentItems.add(GradebookResponse.Item.builder()
                        .id(assignment.getId())
                        .title(assignment.getTitle())
                        .maxMarks(assignment.getTotalMarks())
                        .score(score)
                        .status(status)
                        .dueDate(assignment.getDueDate())
                        .submittedAt(submission != null ? submission.getSubmittedAt() : null)
                        .gradedAt(submission != null ? submission.getGradedAt() : null)
                        .build());
            }

            List<GradebookResponse.Item> assessmentItems = new ArrayList<>();
            for (Assessment assessment : assessments) {
                AssessmentResult result = resultsByKey.get(student.getId() + ":" + assessment.getId());
                Integer score = result != null ? result.getTotalScore() : null;
                if (score != null) {
                    obtained += score;
                    obtainable += assessment.getTotalMarks() != null ? assessment.getTotalMarks() : 0;
                }
                assessmentItems.add(GradebookResponse.Item.builder()
                        .id(assessment.getId())
                        .title(assessment.getTitle())
                        .maxMarks(assessment.getTotalMarks())
                        .score(score)
                        .status(result != null ? "COMPLETED" : "NOT_ATTEMPTED")
                        .dueDate(assessment.getEndsAt())
                        .gradedAt(result != null ? result.getGradedAt() : null)
                        .build());
            }

            BigDecimal average = obtainable > 0
                    ? BigDecimal.valueOf(obtained * 100L).divide(BigDecimal.valueOf(obtainable), 2, RoundingMode.HALF_UP)
                    : null;

            User user = usersById.get(student.getUserId());
            String studentName = user != null
                    ? user.getFirstName() + " " + user.getLastName()
                    : null;

            rows.add(GradebookResponse.Row.builder()
                    .studentId(student.getId())
                    .studentName(studentName)
                    .admissionNumber(student.getAdmissionNumber())
                    .assignments(assignmentItems)
                    .assessments(assessmentItems)
                    .totalObtained(obtained)
                    .totalObtainable(obtainable)
                    .averagePercentage(average)
                    .build());
        }

        return GradebookResponse.builder()
                .classGroupId(classGroupId)
                .className(className)
                .rows(rows)
                .build();
    }
}
