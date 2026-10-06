package tz.elmkusoma.learning.service;

import tz.elmkusoma.learning.dto.request.AssignmentRequest;
import tz.elmkusoma.learning.dto.request.LessonRequest;
import tz.elmkusoma.learning.dto.request.ProgressRequest;
import tz.elmkusoma.learning.dto.request.SubmissionRequest;
import tz.elmkusoma.learning.dto.response.*;

import java.util.List;
import java.util.UUID;

public interface LearningService {

    LessonResponse createLesson(UUID institutionId, LessonRequest request);

    LessonResponse createLesson(UUID institutionId, LessonRequest request, UUID publisherId);

    LessonResponse updateLesson(UUID lessonId, LessonRequest request, UUID institutionId, String userEmail, String userRole);

    LessonResponse updateLesson(UUID lessonId, LessonRequest request, UUID institutionId, String userEmail, String userRole, UUID publisherId);

    void deleteLesson(UUID lessonId, UUID institutionId, String userEmail, String userRole);

    LessonResponse setLessonStatus(UUID lessonId, String status, UUID institutionId, String userEmail, String userRole);

    List<LessonResponse> getLessonsBySubjectAndClass(UUID subjectId, UUID classGroupId, boolean includeUnpublished);

    List<LessonResponse> getLessonsByClass(UUID classGroupId, boolean includeUnpublished);

    ProgressResponse updateProgress(UUID institutionId, UUID studentId, ProgressRequest request);

    List<ProgressResponse> getStudentProgress(UUID studentId);

    Double getStudentAverageCompletion(UUID studentId);

    AssignmentResponse createAssignment(UUID institutionId, AssignmentRequest request);

    /** Create with actor context (audit + publisher-scoped notifications). */
    AssignmentResponse createAssignment(UUID institutionId, AssignmentRequest request, String userEmail, String userRole);

    /** Update with institution + ownership enforcement (creator or admin only). */
    AssignmentResponse updateAssignment(UUID assignmentId, AssignmentRequest request, UUID institutionId, String userEmail, String userRole);

    /** Delete with institution + ownership enforcement (creator or admin only). */
    void deleteAssignment(UUID assignmentId, UUID institutionId, String userEmail, String userRole);

    /** Class-scoped list; learners only receive non-DRAFT items and must be class members. */
    List<AssignmentResponse> getAssignmentsByClass(UUID classGroupId, UUID institutionId, String userEmail, String userRole);

    /**
     * Batch list across several classes in one query (teacher/admin workspace —
     * replaces per-class loops). Institution-scoped; teachers only receive the
     * assignments they created.
     */
    List<AssignmentResponse> getAssignmentsByClasses(List<UUID> classGroupIds, UUID institutionId,
                                                     String userEmail, String userRole);

    /**
     * Submit (or resubmit while ungraded) an assignment. Persists the learner's typed
     * answer and attachment reference. Resolves the caller's student profile so the
     * submission's student_id satisfies the FK to students(id).
     *
     * @param callerUserId the authenticated user id (users.id) from the request context
     */
    SubmissionResponse submitAssignment(UUID assignmentId, UUID callerUserId, UUID institutionId, SubmissionRequest request);

    /** The caller's own submission for an assignment (null when not submitted yet). */
    SubmissionResponse getMySubmission(UUID assignmentId, UUID callerUserId);

    /** Submissions for an assignment; institution + ownership scoped (creator or admin only). */
    List<SubmissionResponse> getSubmissionsByAssignment(UUID assignmentId, UUID institutionId,
                                                        String userEmail, String userRole);

    /** Grade a submission with institution + ownership enforcement (creator or admin only) and a grade-change audit trail. */
    SubmissionResponse gradeSubmission(UUID submissionId, Integer grade, String feedback, UUID gradedBy,
                                       UUID institutionId, String userEmail, String userRole);
}
