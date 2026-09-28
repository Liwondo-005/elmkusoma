package tz.elmkusoma.learning.service;

import tz.elmkusoma.learning.dto.request.AssignmentRequest;
import tz.elmkusoma.learning.dto.request.LessonRequest;
import tz.elmkusoma.learning.dto.request.ProgressRequest;
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

    AssignmentResponse updateAssignment(UUID assignmentId, AssignmentRequest request);

    void deleteAssignment(UUID assignmentId);

    List<AssignmentResponse> getAssignmentsByClass(UUID classGroupId);

    SubmissionResponse submitAssignment(UUID assignmentId, UUID studentId, UUID institutionId);

    List<SubmissionResponse> getSubmissionsByAssignment(UUID assignmentId);

    SubmissionResponse gradeSubmission(UUID submissionId, Integer grade, String feedback, UUID gradedBy);
}
