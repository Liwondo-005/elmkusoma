package tz.elmkusoma.learning.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.learner.service.NotificationService;
import tz.elmkusoma.learning.domain.*;
import tz.elmkusoma.learning.dto.request.AssignmentRequest;
import tz.elmkusoma.learning.dto.request.LessonRequest;
import tz.elmkusoma.learning.dto.request.ProgressRequest;
import tz.elmkusoma.learning.dto.response.*;
import tz.elmkusoma.learning.repository.*;
import tz.elmkusoma.learning.service.LearningService;
import tz.elmkusoma.learner.service.NotificationService;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.student.domain.Student;
import tz.elmkusoma.student.domain.StudentClassAssignment;
import tz.elmkusoma.student.domain.StudentStatus;
import tz.elmkusoma.student.repository.StudentClassAssignmentRepository;
import tz.elmkusoma.student.repository.StudentRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LearningServiceImpl implements LearningService {

    private final LessonRepository lessonRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository submissionRepository;
    private final StudentClassAssignmentRepository studentClassAssignmentRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Override
    public LessonResponse createLesson(UUID institutionId, LessonRequest request) {
        return createLesson(institutionId, request, null);
    }

    @Override
    public LessonResponse createLesson(UUID institutionId, LessonRequest request, UUID publisherId) {
        boolean published = request.getIsPublished() != null && request.getIsPublished();
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
                .isPublished(published)
                .status(published ? "PUBLISHED" : "DRAFT")
                .build();

        Lesson saved = lessonRepository.save(lesson);
        if (Boolean.TRUE.equals(saved.getIsPublished())) {
            notifyLessonPublished(saved, publisherId);
        }
        return toLessonResponse(saved);
    }

    @Override
    public LessonResponse updateLesson(UUID lessonId, LessonRequest request, UUID institutionId, String userEmail, String userRole) {
        return updateLesson(lessonId, request, institutionId, userEmail, userRole, null);
    }

    @Override
    public LessonResponse updateLesson(UUID lessonId, LessonRequest request, UUID institutionId, String userEmail, String userRole, UUID publisherId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .filter(l -> !l.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", lessonId));
        assertCanManage(lesson, institutionId, userEmail, userRole);

        boolean wasPublished = Boolean.TRUE.equals(lesson.getIsPublished());

        if (request.getTitle() != null) lesson.setTitle(request.getTitle());
        if (request.getDescription() != null) lesson.setDescription(request.getDescription());
        if (request.getContentText() != null) lesson.setContentText(request.getContentText());
        if (request.getVideoUrl() != null) lesson.setVideoUrl(request.getVideoUrl());
        if (request.getFileAttachments() != null) lesson.setFileAttachments(request.getFileAttachments());
        if (request.getSortOrder() != null) lesson.setSortOrder(request.getSortOrder());
        if (request.getIsPublished() != null) {
            lesson.setIsPublished(request.getIsPublished());
            lesson.setStatus(request.getIsPublished() ? "PUBLISHED"
                    : ("ARCHIVED".equals(lesson.getStatus()) ? "ARCHIVED" : "DRAFT"));
        }

        Lesson saved = lessonRepository.save(lesson);
        if (Boolean.TRUE.equals(saved.getIsPublished()) && !wasPublished) {
            notifyLessonPublished(saved, publisherId);
        }
        return toLessonResponse(saved);
    }

    /**
     * Notifies the students of the lesson's class group when a lesson becomes PUBLISHED.
     * Falls back to every active student of the institution when the class group has no
     * student assignments. The publisher is never notified.
     */
    private void notifyLessonPublished(Lesson lesson, UUID publisherId) {
        try {
            String title = "New lesson published";
            String message = "New lesson published: " + lesson.getTitle();

            List<UUID> studentUserIds = resolveClassStudentUserIds(lesson.getClassGroupId());
            if (studentUserIds.isEmpty()) {
                notificationService.notifyInstitutionStudentsExcluding(
                        lesson.getInstitutionId(), publisherId, title, message,
                        "LESSON_PUBLISHED", "lesson", lesson.getId());
                return;
            }

            for (UUID studentUserId : studentUserIds) {
                if (studentUserId.equals(publisherId)) {
                    continue;
                }
                notificationService.notifyUser(studentUserId, title, message,
                        "LESSON_PUBLISHED", "lesson", lesson.getId());
            }
            log.info("Sent LESSON_PUBLISHED notifications for lesson {} to {} class students",
                    lesson.getId(), studentUserIds.size());
        } catch (Exception ex) {
            log.warn("Failed to notify students about published lesson {}: {}",
                    lesson.getId(), ex.getMessage());
        }
    }

    /** Resolves the user ids of the active students assigned to the given class group. */
    private List<UUID> resolveClassStudentUserIds(UUID classGroupId) {
        if (classGroupId == null) {
            return List.of();
        }
        Set<UUID> userIds = new LinkedHashSet<>();
        for (StudentClassAssignment assignment
                : studentClassAssignmentRepository.findByClassGroupIdAndIsDeletedFalse(classGroupId)) {
            if (Boolean.FALSE.equals(assignment.getIsActive())) {
                continue;
            }
            Student student = studentRepository.findById(assignment.getStudentId()).orElse(null);
            if (student != null && !Boolean.TRUE.equals(student.getIsDeleted()) && student.getUserId() != null) {
                userIds.add(student.getUserId());
            }
        }
        return new ArrayList<>(userIds);
    }

    @Override
    public void deleteLesson(UUID lessonId, UUID institutionId, String userEmail, String userRole) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .filter(l -> !l.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", lessonId));
        assertCanManage(lesson, institutionId, userEmail, userRole);
        lesson.setIsDeleted(true);
        lessonRepository.save(lesson);
    }

    @Override
    public LessonResponse setLessonStatus(UUID lessonId, String status, UUID institutionId, String userEmail, String userRole) {
        String normalized = status == null ? "" : status.trim().toUpperCase();
        if (!List.of("DRAFT", "READY", "PUBLISHED", "ARCHIVED").contains(normalized)) {
            throw new IllegalArgumentException("Invalid lesson status: " + status);
        }
        Lesson lesson = lessonRepository.findById(lessonId)
                .filter(l -> !l.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", lessonId));
        assertCanManage(lesson, institutionId, userEmail, userRole);

        boolean wasPublished = "PUBLISHED".equals(lesson.getStatus()) || Boolean.TRUE.equals(lesson.getIsPublished());
        boolean willPublish = "PUBLISHED".equals(normalized);

        lesson.setStatus(normalized);
        lesson.setIsPublished(willPublish);
        Lesson saved = lessonRepository.save(lesson);

        if (willPublish && !wasPublished) {
            notifyStudentsOfPublish(saved, institutionId, userEmail);
        }
        return toLessonResponse(saved);
    }

    /**
     * Tells the learners who should see the lesson that it is now live.
     * Audience: students of the lesson's class group (via student_class_assignments
     * -> students -> user). When that mapping yields nobody (no assignment rows for
     * the class group), it falls back to every active STUDENT member of the
     * institution, excluding the publisher.
     */
    private void notifyStudentsOfPublish(Lesson lesson, UUID institutionId, String publisherEmail) {
        try {
            UUID publisherId = publisherEmail == null ? null
                    : userRepository.findByEmailAndIsDeletedFalse(publisherEmail).map(User::getId).orElse(null);
            String title = "New lesson published: " + lesson.getTitle();
            String message = "The lesson \"" + lesson.getTitle()
                    + "\" has been published and is now available in your class.";

            List<UUID> audience = resolveClassGroupAudience(lesson.getClassGroupId());
            if (!audience.isEmpty()) {
                int sent = 0;
                for (UUID studentUserId : audience) {
                    if (studentUserId.equals(publisherId)) {
                        continue;
                    }
                    notificationService.notifyUser(studentUserId, title, message,
                            "LESSON_PUBLISHED", "lesson", lesson.getId());
                    sent++;
                }
                log.info("Lesson {} published: notified {} student(s) of class group {}",
                        lesson.getId(), sent, lesson.getClassGroupId());
                return;
            }

            if (institutionId != null) {
                notificationService.notifyInstitutionStudentsExcluding(institutionId, publisherId, title, message,
                        "LESSON_PUBLISHED", "lesson", lesson.getId());
                log.info("Lesson {} published: class group {} has no students - "
                                + "notified active institution students instead",
                        lesson.getId(), lesson.getClassGroupId());
            }
        } catch (Exception e) {
            log.warn("Failed to send publish notification for lesson {}: {}", lesson.getId(), e.getMessage());
        }
    }

    /** User ids of active students assigned to the given class group. */
    private List<UUID> resolveClassGroupAudience(UUID classGroupId) {
        if (classGroupId == null) {
            return List.of();
        }
        List<UUID> studentUserIds = new ArrayList<>();
        for (var assignment : studentClassAssignmentRepository.findByClassGroupIdAndIsDeletedFalse(classGroupId)) {
            if (!Boolean.TRUE.equals(assignment.getIsActive())) {
                continue;
            }
            Student student = studentRepository.findById(assignment.getStudentId()).orElse(null);
            if (student == null || Boolean.TRUE.equals(student.getIsDeleted())
                    || student.getStatus() != StudentStatus.ACTIVE) {
                continue;
            }
            studentUserIds.add(student.getUserId());
        }
        return studentUserIds;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LessonResponse> getLessonsBySubjectAndClass(UUID subjectId, UUID classGroupId, boolean includeUnpublished) {
        return lessonRepository.findBySubjectIdAndClassGroupIdAndIsDeletedFalseOrderBySortOrder(subjectId, classGroupId)
                .stream()
                .filter(l -> includeUnpublished || !"ARCHIVED".equals(l.getStatus()))
                .filter(l -> includeUnpublished || Boolean.TRUE.equals(l.getIsPublished()))
                .map(this::toLessonResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LessonResponse> getLessonsByClass(UUID classGroupId, boolean includeUnpublished) {
        return lessonRepository.findByClassGroupIdAndIsDeletedFalseOrderBySortOrder(classGroupId)
                .stream()
                .filter(l -> includeUnpublished || !"ARCHIVED".equals(l.getStatus()))
                .filter(l -> includeUnpublished || Boolean.TRUE.equals(l.getIsPublished()))
                .map(this::toLessonResponse).toList();
    }

    /** Institution + ownership enforcement: creator or admin only. */
    private void assertCanManage(Lesson lesson, UUID institutionId, String userEmail, String userRole) {
        if (institutionId == null || !institutionId.equals(lesson.getInstitutionId())) {
            throw new ResourceNotFoundException("Lesson", "id", lesson.getId());
        }
        boolean isAdmin = "ADMIN".equals(userRole) || "INSTITUTION_ADMIN".equals(userRole)
                || "NATIONAL_ADMIN".equals(userRole);
        if (isAdmin) return;
        String owner = lesson.getCreatedBy();
        if (owner == null || "system".equals(owner) || userEmail == null || !owner.equalsIgnoreCase(userEmail)) {
            throw new SecurityException("You do not own this lesson");
        }
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
                .status(l.getStatus())
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
