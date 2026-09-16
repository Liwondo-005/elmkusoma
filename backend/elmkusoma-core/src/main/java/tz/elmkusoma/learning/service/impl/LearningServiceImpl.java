package tz.elmkusoma.learning.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.learning.domain.*;
import tz.elmkusoma.learning.dto.request.AssignmentRequest;
import tz.elmkusoma.learning.dto.request.LessonRequest;
import tz.elmkusoma.learning.dto.request.ProgressRequest;
import tz.elmkusoma.learning.dto.response.*;
import tz.elmkusoma.learning.repository.*;
import tz.elmkusoma.learning.service.LearningService;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.student.domain.Student;
import tz.elmkusoma.student.repository.StudentClassAssignmentRepository;
import tz.elmkusoma.student.repository.StudentRepository;

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
    private final StudentClassAssignmentRepository studentClassAssignmentRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;

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
    public LessonResponse updateLesson(UUID lessonId, LessonRequest request) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .filter(l -> !l.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", lessonId));

        if (request.getTitle() != null) lesson.setTitle(request.getTitle());
        if (request.getDescription() != null) lesson.setDescription(request.getDescription());
        if (request.getContentText() != null) lesson.setContentText(request.getContentText());
        if (request.getVideoUrl() != null) lesson.setVideoUrl(request.getVideoUrl());
        if (request.getFileAttachments() != null) lesson.setFileAttachments(request.getFileAttachments());
        if (request.getSortOrder() != null) lesson.setSortOrder(request.getSortOrder());
        if (request.getIsPublished() != null) lesson.setIsPublished(request.getIsPublished());

        return toLessonResponse(lessonRepository.save(lesson));
    }

    @Override
    public void deleteLesson(UUID lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .filter(l -> !l.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", lessonId));
        lesson.setIsDeleted(true);
        lessonRepository.save(lesson);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LessonResponse> getLessonsBySubjectAndClass(UUID subjectId, UUID classGroupId) {
        return lessonRepository.findBySubjectIdAndClassGroupIdAndIsDeletedFalseOrderBySortOrder(subjectId, classGroupId)
                .stream().map(this::toLessonResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LessonResponse> getLessonsByClass(UUID classGroupId) {
        return lessonRepository.findByClassGroupIdAndIsDeletedFalseOrderBySortOrder(classGroupId)
                .stream().map(this::toLessonResponse).toList();
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
    public List<ProgressResponse> getStudentProgress(UUID studentId) {
        return lessonProgressRepository.findByStudentIdAndIsDeletedFalse(studentId)
                .stream().map(this::toProgressResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Double getStudentAverageCompletion(UUID studentId) {
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
                .assignmentType(request.getAssignmentType())
                .instructions(request.getInstructions())
                .status(request.getStatus() != null ? request.getStatus() : "PUBLISHED")
                .build();

        return toAssignmentResponse(assignmentRepository.save(assignment));
    }

    @Override
    public AssignmentResponse updateAssignment(UUID assignmentId, AssignmentRequest request) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .filter(a -> !a.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Assignment", "id", assignmentId));

        if (request.getTitle() != null) assignment.setTitle(request.getTitle());
        if (request.getDescription() != null) assignment.setDescription(request.getDescription());
        if (request.getDueDate() != null) assignment.setDueDate(request.getDueDate());
        if (request.getTotalMarks() != null) assignment.setTotalMarks(request.getTotalMarks());
        if (request.getAttachments() != null) assignment.setAttachments(request.getAttachments());
        if (request.getSubjectId() != null) assignment.setSubjectId(request.getSubjectId());
        if (request.getClassGroupId() != null) assignment.setClassGroupId(request.getClassGroupId());
        if (request.getAssignmentType() != null) assignment.setAssignmentType(request.getAssignmentType());
        if (request.getInstructions() != null) assignment.setInstructions(request.getInstructions());
        if (request.getStatus() != null) assignment.setStatus(request.getStatus());

        return toAssignmentResponse(assignmentRepository.save(assignment));
    }

    @Override
    public void deleteAssignment(UUID assignmentId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .filter(a -> !a.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Assignment", "id", assignmentId));
        assignment.setIsDeleted(true);
        assignmentRepository.save(assignment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentResponse> getAssignmentsByClass(UUID classGroupId) {
        return assignmentRepository.findByClassGroupIdAndIsDeletedFalse(classGroupId)
                .stream().map(this::toAssignmentResponse).toList();
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
    public List<SubmissionResponse> getSubmissionsByAssignment(UUID assignmentId) {
        return submissionRepository.findByAssignmentIdAndIsDeletedFalse(assignmentId)
                .stream().map(this::toSubmissionResponse).toList();
    }

    @Override
    public SubmissionResponse gradeSubmission(UUID submissionId, Integer grade, String feedback, UUID gradedBy) {
        AssignmentSubmission submission = submissionRepository.findById(submissionId)
                .filter(s -> !s.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found with id: " + submissionId));

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
        long submissionCount = submissionRepository.countByAssignmentIdAndIsDeletedFalse(a.getId());
        long totalStudents = studentClassAssignmentRepository.countActiveByClassGroupId(a.getClassGroupId());
        return AssignmentResponse.builder()
                .id(a.getId())
                .subjectId(a.getSubjectId())
                .classGroupId(a.getClassGroupId())
                .title(a.getTitle())
                .description(a.getDescription())
                .dueDate(a.getDueDate())
                .totalMarks(a.getTotalMarks())
                .attachments(a.getAttachments())
                .assignmentType(a.getAssignmentType())
                .instructions(a.getInstructions())
                .status(a.getStatus())
                .submissionCount((int) submissionCount)
                .totalStudents((int) totalStudents)
                .createdAt(a.getCreatedAt())
                .build();
    }

    private SubmissionResponse toSubmissionResponse(AssignmentSubmission s) {
        String studentName = null;
        Student student = studentRepository.findById(s.getStudentId()).orElse(null);
        if (student != null) {
            User user = userRepository.findById(student.getUserId()).orElse(null);
            if (user != null) {
                studentName = user.getFirstName() + " " + user.getLastName();
            }
        }
        return SubmissionResponse.builder()
                .id(s.getId())
                .assignmentId(s.getAssignmentId())
                .studentId(s.getStudentId())
                .studentName(studentName)
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
