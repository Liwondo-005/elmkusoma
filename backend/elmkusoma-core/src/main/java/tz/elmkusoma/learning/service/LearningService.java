package tz.elmkusoma.learning.service;

import tz.elmkusoma.learning.dto.request.AssignmentRequest;
import tz.elmkusoma.learning.dto.request.LessonRequest;
import tz.elmkusoma.learning.dto.request.ProgressRequest;
import tz.elmkusoma.learning.dto.response.*;

import java.util.List;
import java.util.UUID;

public interface LearningService {

    LessonResponse createLesson(UUID institutionId, LessonRequest request);

    List<LessonResponse> getLessonsBySubjectAndClass(UUID subjectId, UUID classGroupId, UUID institutionId);

    List<LessonResponse> getLessonsByClass(UUID classGroupId, UUID institutionId);

    ProgressResponse updateProgress(UUID institutionId, UUID studentId, ProgressRequest request);

    List<ProgressResponse> getStudentProgress(UUID studentId, UUID institutionId);

    Double getStudentAverageCompletion(UUID studentId, UUID institutionId);

    AssignmentResponse createAssignment(UUID institutionId, AssignmentRequest request);

    List<AssignmentResponse> getAssignmentsByClass(UUID classGroupId, UUID institutionId);

    SubmissionResponse submitAssignment(UUID assignmentId, UUID studentId, UUID institutionId);

    List<SubmissionResponse> getSubmissionsByAssignment(UUID assignmentId, UUID institutionId);

    SubmissionResponse gradeSubmission(UUID submissionId, Integer grade, String feedback, UUID gradedBy, UUID institutionId);
}
