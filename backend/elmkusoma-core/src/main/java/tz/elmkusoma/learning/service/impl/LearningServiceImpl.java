package tz.elmkusoma.learning.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.audit.domain.AuditLog;
import tz.elmkusoma.audit.service.AuditService;
import tz.elmkusoma.common.ClassAccessGuard;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.learner.service.NotificationService;
import tz.elmkusoma.learning.domain.*;
import tz.elmkusoma.learning.dto.request.AssignmentRequest;
import tz.elmkusoma.learning.dto.request.LessonRequest;
import tz.elmkusoma.learning.dto.request.ProgressRequest;
import tz.elmkusoma.learning.dto.request.SubmissionRequest;
import tz.elmkusoma.learning.dto.response.*;
import tz.elmkusoma.learning.repository.*;
import tz.elmkusoma.learning.service.LearningService;
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
import java.util.Map;
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
    private final AuditService auditService;
    private final ClassAccessGuard classAccessGuard;

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

            List<UUID> studentUserIds = classAccessGuard.resolveClassStudentUserIds(lesson.getClassGroupId());
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
        String oldStatus = lesson.getStatus();

        lesson.setStatus(normalized);
        lesson.setIsPublished(willPublish);
        Lesson saved = lessonRepository.save(lesson);

        if (willPublish && !wasPublished) {
            notifyStudentsOfPublish(saved, institutionId, userEmail);
        }
        auditSafely(institutionId, userEmail, userRole, "Lesson", saved.getId(), saved.getTitle(),
                AuditLog.AuditAction.UPDATE,
                Map.of("status", String.valueOf(oldStatus)),
                Map.of("status", normalized));
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

            List<UUID> audience = classAccessGuard.resolveClassStudentUserIds(lesson.getClassGroupId());
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
        UUID resolvedStudentId = classAccessGuard.resolveStudentId(studentId);
        if (resolvedStudentId == null) {
            throw new SecurityException("A student profile is required to record progress");
        }
        LessonProgress progress = lessonProgressRepository
                .findByLessonIdAndStudentIdAndIsDeletedFalse(request.getLessonId(), resolvedStudentId)
                .orElse(null);

        if (progress == null) {
            progress = LessonProgress.builder()
                    .institutionId(institutionId)
                    .lessonId(request.getLessonId())
                    .studentId(resolvedStudentId)
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
        UUID resolved = classAccessGuard.resolveStudentId(studentId);
        if (resolved == null) {
            return List.of();
        }
        return lessonProgressRepository.findByStudentIdAndIsDeletedFalse(resolved)
                .stream().map(this::toProgressResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Double getStudentAverageCompletion(UUID studentId) {
        UUID resolved = classAccessGuard.resolveStudentId(studentId);
        if (resolved == null) {
            return 0.0;
        }
        return lessonProgressRepository.getAverageCompletionByStudent(resolved);
    }

    @Override
    public AssignmentResponse createAssignment(UUID institutionId, AssignmentRequest request) {
        return createAssignment(institutionId, request, null, null);
    }

    @Override
    public AssignmentResponse createAssignment(UUID institutionId, AssignmentRequest request, String userEmail, String userRole) {
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
                .lessonId(request.getLessonId())
                .openDate(request.getOpenDate())
                .closeDate(request.getCloseDate())
                .allowLateSubmission(Boolean.TRUE.equals(request.getAllowLateSubmission()))
                .build();

        Assignment saved = assignmentRepository.save(assignment);

        if ("PUBLISHED".equalsIgnoreCase(saved.getStatus())) {
            notifyAssignmentPublished(saved, resolveActorUserId(userEmail));
        }
        auditSafely(institutionId, userEmail, userRole, "Assignment", saved.getId(), saved.getTitle(),
                AuditLog.AuditAction.CREATE, null,
                Map.of("title", saved.getTitle(), "classGroupId", String.valueOf(saved.getClassGroupId())));
        return toAssignmentResponse(saved);
    }

    @Override
    public AssignmentResponse updateAssignment(UUID assignmentId, AssignmentRequest request,
                                               UUID institutionId, String userEmail, String userRole) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .filter(a -> !a.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Assignment", "id", assignmentId));
        assertCanManageAssignment(assignment, institutionId, userEmail, userRole);

        Map<String, Object> oldValues = Map.of(
                "title", assignment.getTitle(),
                "dueDate", String.valueOf(assignment.getDueDate()),
                "status", String.valueOf(assignment.getStatus()));
        boolean wasPublished = "PUBLISHED".equalsIgnoreCase(assignment.getStatus());

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
        if (request.getLessonId() != null) assignment.setLessonId(request.getLessonId());
        if (request.getOpenDate() != null) assignment.setOpenDate(request.getOpenDate());
        if (request.getCloseDate() != null) assignment.setCloseDate(request.getCloseDate());
        if (request.getAllowLateSubmission() != null) assignment.setAllowLateSubmission(request.getAllowLateSubmission());

        Assignment saved = assignmentRepository.save(assignment);

        if (!wasPublished && "PUBLISHED".equalsIgnoreCase(saved.getStatus())) {
            notifyAssignmentPublished(saved, resolveActorUserId(userEmail));
        }
        auditSafely(institutionId, userEmail, userRole, "Assignment", saved.getId(), saved.getTitle(),
                AuditLog.AuditAction.UPDATE, oldValues,
                Map.of("title", saved.getTitle(), "status", String.valueOf(saved.getStatus())));
        return toAssignmentResponse(saved);
    }

    @Override
    public void deleteAssignment(UUID assignmentId, UUID institutionId, String userEmail, String userRole) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .filter(a -> !a.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Assignment", "id", assignmentId));
        assertCanManageAssignment(assignment, institutionId, userEmail, userRole);
        assignment.setIsDeleted(true);
        assignmentRepository.save(assignment);
        auditSafely(institutionId, userEmail, userRole, "Assignment", assignmentId, assignment.getTitle(),
                AuditLog.AuditAction.DELETE, Map.of("title", assignment.getTitle()), null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentResponse> getAssignmentsByClass(UUID classGroupId, UUID institutionId,
                                                          String userEmail, String userRole) {
        boolean isLearner = isLearnerRole(userRole);
        if (isLearner) {
            classAccessGuard.assertLearnerCanAccessClass(userEmail, classGroupId);
        }
        return assignmentRepository.findByClassGroupIdAndIsDeletedFalse(classGroupId)
                .stream()
                .filter(a -> institutionId == null || institutionId.equals(a.getInstitutionId()))
                .filter(a -> !isLearner || !"DRAFT".equalsIgnoreCase(a.getStatus()))
                .map(this::toAssignmentResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentResponse> getAssignmentsByClasses(List<UUID> classGroupIds, UUID institutionId) {
        if (classGroupIds == null || classGroupIds.isEmpty()) {
            return List.of();
        }
        return assignmentRepository.findByClassGroupIdInAndIsDeletedFalse(classGroupIds)
                .stream()
                .filter(a -> institutionId == null || institutionId.equals(a.getInstitutionId()))
                .map(this::toAssignmentResponse)
                .toList();
    }

    @Override
    public SubmissionResponse submitAssignment(UUID assignmentId, UUID callerUserId, UUID institutionId,
                                               SubmissionRequest request) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .filter(a -> !a.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found with id: " + assignmentId));
        if (institutionId != null && !institutionId.equals(assignment.getInstitutionId())) {
            throw new ResourceNotFoundException("Assignment not found with id: " + assignmentId);
        }

        Student student = classAccessGuard.findStudentByUserId(callerUserId);
        if (student == null) {
            throw new SecurityException("A student profile is required to submit an assignment");
        }

        boolean draft = request != null && Boolean.TRUE.equals(request.getDraft());
        LocalDateTime now = LocalDateTime.now();
        validateSubmissionWindow(assignment, now, draft);

        AssignmentSubmission submission = submissionRepository
                .findByAssignmentIdAndStudentIdAndIsDeletedFalse(assignmentId, student.getId())
                .orElse(null);

        boolean isResubmission = submission != null;
        if (isResubmission && "GRADED".equalsIgnoreCase(submission.getStatus())) {
            throw new IllegalStateException("This submission has already been graded and can no longer be changed");
        }

        boolean late = assignment.getDueDate() != null && now.isAfter(assignment.getDueDate());
        String content = request == null ? null : request.getContent();
        String fileUrl = request == null ? null : request.getFileUrl();

        if (submission == null) {
            submission = AssignmentSubmission.builder()
                    .institutionId(assignment.getInstitutionId())
                    .assignmentId(assignmentId)
                    .studentId(student.getId())
                    .build();
        }
        if (content != null && !content.isBlank()) {
            submission.setSubmissionText(content);
        }
        if (fileUrl != null && !fileUrl.isBlank()) {
            submission.setFileUrl(fileUrl);
        }
        submission.setIsDraft(draft);
        submission.setStatus(draft ? "DRAFT" : "SUBMITTED");
        submission.setIsLate(!draft && late);
        submission.setSubmittedAt(now);

        AssignmentSubmission saved = submissionRepository.save(submission);

        if (!draft) {
            notifyTeacherOfSubmission(assignment, saved);
        }
        auditSafely(assignment.getInstitutionId(), null, null, "AssignmentSubmission", saved.getId(),
                assignment.getTitle() + (draft ? " (draft)" : ""),
                isResubmission ? AuditLog.AuditAction.UPDATE : AuditLog.AuditAction.CREATE,
                isResubmission ? Map.of("status", String.valueOf(saved.getStatus())) : null,
                Map.of("assignmentId", String.valueOf(assignmentId),
                        "status", saved.getStatus(),
                        "late", String.valueOf(Boolean.TRUE.equals(saved.getIsLate()))));
        return toSubmissionResponse(saved);
    }

    /** Open/close/due-date enforcement. Drafts bypass the due/close window (work in progress). */
    private void validateSubmissionWindow(Assignment assignment, LocalDateTime now, boolean draft) {
        if (draft) {
            return;
        }
        if (assignment.getOpenDate() != null && now.isBefore(assignment.getOpenDate())) {
            throw new IllegalStateException("This assignment is not open for submissions yet");
        }
        if (assignment.getCloseDate() != null && now.isAfter(assignment.getCloseDate())) {
            throw new IllegalStateException("The submission window for this assignment has closed");
        }
        if (assignment.getDueDate() != null && now.isAfter(assignment.getDueDate())
                && !Boolean.TRUE.equals(assignment.getAllowLateSubmission())) {
            throw new IllegalStateException("The due date for this assignment has passed");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public SubmissionResponse getMySubmission(UUID assignmentId, UUID callerUserId) {
        Student student = classAccessGuard.findStudentByUserId(callerUserId);
        if (student == null) {
            return null;
        }
        return submissionRepository.findByAssignmentIdAndStudentIdAndIsDeletedFalse(assignmentId, student.getId())
                .map(this::toSubmissionResponse)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubmissionResponse> getSubmissionsByAssignment(UUID assignmentId, UUID institutionId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .filter(a -> !a.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Assignment", "id", assignmentId));
        if (institutionId != null && !institutionId.equals(assignment.getInstitutionId())) {
            throw new ResourceNotFoundException("Assignment", "id", assignmentId);
        }
        return submissionRepository.findByAssignmentIdAndIsDeletedFalse(assignmentId)
                .stream().map(this::toSubmissionResponse).toList();
    }

    @Override
    public SubmissionResponse gradeSubmission(UUID submissionId, Integer grade, String feedback, UUID gradedBy,
                                              UUID institutionId, String userEmail, String userRole) {
        AssignmentSubmission submission = submissionRepository.findById(submissionId)
                .filter(s -> !s.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found with id: " + submissionId));

        Assignment assignment = assignmentRepository.findById(submission.getAssignmentId())
                .filter(a -> !a.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Assignment", "id", submission.getAssignmentId()));
        if (institutionId != null && !institutionId.equals(assignment.getInstitutionId())) {
            throw new ResourceNotFoundException("Submission not found with id: " + submissionId);
        }

        Integer oldGrade = submission.getGrade();
        submission.setGrade(grade);
        submission.setFeedback(feedback);
        submission.setGradedAt(LocalDateTime.now());
        submission.setGradedBy(gradedBy);
        submission.setStatus("GRADED");
        submission.setIsDraft(false);

        AssignmentSubmission saved = submissionRepository.save(submission);

        notifyStudentOfGrade(assignment, saved);
        // Grade-change audit trail (old grade -> new grade, actor, timestamp via audit row).
        auditSafely(assignment.getInstitutionId(), userEmail, userRole, "AssignmentSubmission", saved.getId(),
                assignment.getTitle(), AuditLog.AuditAction.UPDATE,
                Map.of("grade", String.valueOf(oldGrade), "feedback", String.valueOf(submission.getFeedback())),
                Map.of("grade", String.valueOf(grade),
                        "feedback", String.valueOf(feedback),
                        "source", "ASSIGNMENT_GRADING",
                        "changedBy", String.valueOf(userEmail)));
        return toSubmissionResponse(saved);
    }

    // ── Assignment helpers ────────────────────────────────────────────────────

    /** Institution + ownership enforcement for assignment mutation: creator or admin only. */
    private void assertCanManageAssignment(Assignment assignment, UUID institutionId, String userEmail, String userRole) {
        if (institutionId == null || !institutionId.equals(assignment.getInstitutionId())) {
            throw new ResourceNotFoundException("Assignment", "id", assignment.getId());
        }
        if (isAdminRole(userRole)) {
            return;
        }
        String owner = assignment.getCreatedBy();
        if (owner == null || "system".equals(owner) || userEmail == null || !owner.equalsIgnoreCase(userEmail)) {
            throw new SecurityException("You do not own this assignment");
        }
    }

    private static boolean isAdminRole(String role) {
        return "ADMIN".equals(role) || "INSTITUTION_ADMIN".equals(role) || "NATIONAL_ADMIN".equals(role);
    }

    private static boolean isLearnerRole(String role) {
        return "STUDENT".equals(role) || "OTHER_LEARNER".equals(role);
    }

    private UUID resolveActorUserId(String userEmail) {
        if (userEmail == null) {
            return null;
        }
        return userRepository.findByEmailAndIsDeletedFalse(userEmail).map(User::getId).orElse(null);
    }

    /** Notifies the class learners when a new assignment becomes available. */
    private void notifyAssignmentPublished(Assignment assignment, UUID publisherId) {
        try {
            String title = "New assignment: " + assignment.getTitle();
            String message = "A new assignment \"" + assignment.getTitle() + "\" is available for your class.";
            List<UUID> studentUserIds = classAccessGuard.resolveClassStudentUserIds(assignment.getClassGroupId());
            if (studentUserIds.isEmpty()) {
                notificationService.notifyInstitutionStudentsExcluding(
                        assignment.getInstitutionId(), publisherId, title, message,
                        "ASSIGNMENT_PUBLISHED", "assignment", assignment.getId());
                return;
            }
            int sent = 0;
            for (UUID studentUserId : studentUserIds) {
                if (studentUserId.equals(publisherId)) {
                    continue;
                }
                notificationService.notifyUser(studentUserId, title, message,
                        "ASSIGNMENT_PUBLISHED", "assignment", assignment.getId());
                sent++;
            }
            log.info("Assignment {} published: notified {} student(s)", assignment.getId(), sent);
        } catch (Exception ex) {
            log.warn("Failed to send ASSIGNMENT_PUBLISHED for assignment {}: {}",
                    assignment.getId(), ex.getMessage());
        }
    }

    /** Tells the assignment's teacher that a learner submitted work. */
    private void notifyTeacherOfSubmission(Assignment assignment, AssignmentSubmission submission) {
        try {
            if (assignment.getCreatedBy() == null || "system".equals(assignment.getCreatedBy())) {
                return;
            }
            UUID teacherUserId = userRepository.findByEmailAndIsDeletedFalse(assignment.getCreatedBy())
                    .map(User::getId).orElse(null);
            if (teacherUserId == null) {
                return;
            }
            notificationService.notifyUser(teacherUserId,
                    "Assignment submitted: " + assignment.getTitle(),
                    "A learner submitted work for \"" + assignment.getTitle() + "\".",
                    "ASSIGNMENT_SUBMITTED", "assignment", assignment.getId());
        } catch (Exception ex) {
            log.warn("Failed to send ASSIGNMENT_SUBMITTED for assignment {}: {}",
                    assignment.getId(), ex.getMessage());
        }
    }

    /** Tells the learner their work was graded and feedback is ready. */
    private void notifyStudentOfGrade(Assignment assignment, AssignmentSubmission submission) {
        try {
            Student student = studentRepository.findById(submission.getStudentId()).orElse(null);
            if (student == null || student.getUserId() == null) {
                return;
            }
            notificationService.notifyUser(student.getUserId(),
                    "Assignment graded: " + assignment.getTitle(),
                    "Your submission for \"" + assignment.getTitle() + "\" has been graded. Check your feedback.",
                    "ASSIGNMENT_GRADED", "submission", submission.getId());
        } catch (Exception ex) {
            log.warn("Failed to send ASSIGNMENT_GRADED for submission {}: {}",
                    submission.getId(), ex.getMessage());
        }
    }

    /** Audit write that can never fail the business transaction (null-safe in unit tests). */
    private void auditSafely(UUID institutionId, String userEmail, String userRole,
                             String entityType, UUID entityId, String entityName,
                             AuditLog.AuditAction action,
                             Map<String, Object> oldValues, Map<String, Object> newValues) {
        try {
            if (auditService == null) {
                return;
            }
            UUID actorId = resolveActorUserId(userEmail);
            auditService.recordAuditLog(institutionId, actorId, userEmail, userRole,
                    entityType, entityId, entityName, action, oldValues, newValues);
        } catch (Exception ex) {
            log.warn("Audit write failed for {} {}: {}", entityType, entityId, ex.getMessage());
        }
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
                .lessonId(a.getLessonId())
                .openDate(a.getOpenDate())
                .closeDate(a.getCloseDate())
                .allowLateSubmission(a.getAllowLateSubmission())
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
                .submissionText(s.getSubmissionText())
                .status(s.getStatus())
                .isDraft(s.getIsDraft())
                .isLate(s.getIsLate())
                .build();
    }
}
