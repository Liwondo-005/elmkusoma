package tz.elmkusoma.learning.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.shared.security.OwnershipGuard;
import tz.elmkusoma.learning.domain.*;
import tz.elmkusoma.learning.dto.request.AssignmentRequest;
import tz.elmkusoma.learning.dto.request.LessonRequest;
import tz.elmkusoma.learning.dto.request.ProgressRequest;
import tz.elmkusoma.learning.dto.response.*;
import tz.elmkusoma.learning.repository.*;
import tz.elmkusoma.learning.service.LearningService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class LearningServiceImpl implements LearningService {

    private final LessonRepository lessonRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository submissionRepository;

    @Override
    public LessonResponse createLesson(UUID institutionId, LessonRequest request) {
        Lesson lesson = Lesson.builder()
                .institutionId(institutionId)
                .subjectId(request.getSubjectId())
                .classGroupId(request.getClassGroupId())
                .title(request.getTitle())
                .description(request.getDescription())
                .contentText(request.getContentText())
                .videoUrl(request.getVideoUrl())
                .fileAttachments(request.getFileAttachments())
                .sortOrder(request.getSortOrder())
                .isPublished(request.getIsPublished() != null ? request.getIsPublished() : false)
                .build();

        return toLessonResponse(lessonRepository.save(lesson));
    }

    @Override
    @Transactional(readOnly = true)
    public List<LessonResponse> getLessonsBySubjectAndClass(UUID subjectId, UUID classGroupId, UUID institutionId) {
        return lessonRepository.findBySubjectIdAndClassGroupIdAndIsDeletedFalseOrderBySortOrder(subjectId, classGroupId)
                .stream()
                .filter(l -> l.getInstitutionId().equals(institutionId))
                .map(this::toLessonResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LessonResponse> getLessonsByClass(UUID classGroupId, UUID institutionId) {
        return lessonRepository.findByClassGroupIdAndIsDeletedFalseOrderBySortOrder(classGroupId)
                .stream()
                .filter(l -> l.getInstitutionId().equals(institutionId))
                .map(this::toLessonResponse)
                .toList();
    }

    @Override
    public ProgressResponse updateProgress(UUID institutionId, UUID studentId, ProgressRequest request) {
        LessonProgress progress = lessonProgressRepository
                .findByLessonIdAndStudentIdAndIsDeletedFalse(request.getLessonId(), studentId)
                .orElse(null);

        if (progress == null) {
            progress = LessonProgress.builder()
                    .institutionId(institutionId)
                    .lessonId(request.getLessonId())
                    .studentId(studentId)
                    .completionPercentage(request.getCompletionPercentage())
                    .startedAt(LocalDateTime.now())
                    .build();
        } else {
            progress.setCompletionPercentage(request.getCompletionPercentage());
        }

        if (request.getCompletionPercentage() >= 100.0 && progress.getCompletedAt() == null) {
            progress.setCompletedAt(LocalDateTime.now());
        }

        return toProgressResponse(lessonProgressRepository.save(progress));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProgressResponse> getStudentProgress(UUID studentId, UUID institutionId) {
        return lessonProgressRepository.findByStudentIdAndIsDeletedFalse(studentId)
                .stream()
                .filter(p -> p.getInstitutionId().equals(institutionId))
                .map(this::toProgressResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Double getStudentAverageCompletion(UUID studentId, UUID institutionId) {
        return lessonProgressRepository.getAverageCompletionByStudent(studentId);
    }

    @Override
    public AssignmentResponse createAssignment(UUID institutionId, AssignmentRequest request) {
        Assignment assignment = Assignment.builder()
                .institutionId(institutionId)
                .subjectId(request.getSubjectId())
                .classGroupId(request.getClassGroupId())
                .title(request.getTitle())
                .description(request.getDescription())
                .dueDate(request.getDueDate())
                .totalMarks(request.getTotalMarks())
                .attachments(request.getAttachments())
                .build();

        return toAssignmentResponse(assignmentRepository.save(assignment));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentResponse> getAssignmentsByClass(UUID classGroupId, UUID institutionId) {
        return assignmentRepository.findByClassGroupIdAndIsDeletedFalse(classGroupId)
                .stream()
                .filter(a -> a.getInstitutionId().equals(institutionId))
                .map(this::toAssignmentResponse)
                .toList();
    }

    @Override
    public SubmissionResponse submitAssignment(UUID assignmentId, UUID studentId, UUID institutionId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .filter(a -> !a.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found with id: " + assignmentId));

        boolean alreadySubmitted = submissionRepository
                .findByAssignmentIdAndStudentIdAndIsDeletedFalse(assignmentId, studentId)
                .isPresent();

        if (alreadySubmitted) {
            throw new IllegalArgumentException("Student has already submitted this assignment");
        }

        AssignmentSubmission submission = AssignmentSubmission.builder()
                .institutionId(institutionId)
                .assignmentId(assignmentId)
                .studentId(studentId)
                .submittedAt(LocalDateTime.now())
                .build();

        return toSubmissionResponse(submissionRepository.save(submission));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubmissionResponse> getSubmissionsByAssignment(UUID assignmentId, UUID institutionId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .filter(a -> !a.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found with id: " + assignmentId));
        OwnershipGuard.verifyInstitution(assignment.getInstitutionId(), institutionId, "assignment");
        return submissionRepository.findByAssignmentIdAndIsDeletedFalse(assignmentId)
                .stream().map(this::toSubmissionResponse).toList();
    }

    @Override
    public SubmissionResponse gradeSubmission(UUID submissionId, Integer grade, String feedback, UUID gradedBy, UUID institutionId) {
        AssignmentSubmission submission = submissionRepository.findById(submissionId)
                .filter(s -> !s.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found with id: " + submissionId));
        OwnershipGuard.verifyInstitution(submission.getInstitutionId(), institutionId, "assignment submission");

        submission.setGrade(grade);
        submission.setFeedback(feedback);
        submission.setGradedAt(LocalDateTime.now());
        submission.setGradedBy(gradedBy);

        return toSubmissionResponse(submissionRepository.save(submission));
    }

    private LessonResponse toLessonResponse(Lesson l) {
        return LessonResponse.builder()
                .id(l.getId())
                .subjectId(l.getSubjectId())
                .classGroupId(l.getClassGroupId())
                .title(l.getTitle())
                .description(l.getDescription())
                .contentText(l.getContentText())
                .videoUrl(l.getVideoUrl())
                .fileAttachments(l.getFileAttachments())
                .sortOrder(l.getSortOrder())
                .isPublished(l.getIsPublished())
                .createdAt(l.getCreatedAt())
                .build();
    }

    private ProgressResponse toProgressResponse(LessonProgress p) {
        return ProgressResponse.builder()
                .id(p.getId())
                .lessonId(p.getLessonId())
                .studentId(p.getStudentId())
                .completionPercentage(p.getCompletionPercentage())
                .startedAt(p.getStartedAt())
                .completedAt(p.getCompletedAt())
                .createdAt(p.getCreatedAt())
                .build();
    }

    private AssignmentResponse toAssignmentResponse(Assignment a) {
        return AssignmentResponse.builder()
                .id(a.getId())
                .subjectId(a.getSubjectId())
                .classGroupId(a.getClassGroupId())
                .title(a.getTitle())
                .description(a.getDescription())
                .dueDate(a.getDueDate())
                .totalMarks(a.getTotalMarks())
                .attachments(a.getAttachments())
                .createdAt(a.getCreatedAt())
                .build();
    }

    private SubmissionResponse toSubmissionResponse(AssignmentSubmission s) {
        return SubmissionResponse.builder()
                .id(s.getId())
                .assignmentId(s.getAssignmentId())
                .studentId(s.getStudentId())
                .fileUrl(s.getFileUrl())
                .submittedAt(s.getSubmittedAt())
                .grade(s.getGrade())
                .feedback(s.getFeedback())
                .gradedAt(s.getGradedAt())
                .gradedBy(s.getGradedBy())
                .createdAt(s.getCreatedAt())
                .build();
    }
}
