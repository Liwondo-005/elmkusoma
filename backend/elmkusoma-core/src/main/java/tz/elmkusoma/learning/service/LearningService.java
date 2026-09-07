package tz.elmkusoma.learning.service;

import tz.elmkusoma.learning.dto.request.AssignmentRequest;
import tz.elmkusoma.learning.dto.request.LessonRequest;
import tz.elmkusoma.learning.dto.request.ProgressRequest;
import tz.elmkusoma.learning.dto.response.*;

import java.util.List;
import java.util.UUID;

public interface LearningService {

    LessonResponse createLesson(UUID institutionId, LessonRequest request);

    List<LessonResponse> getLessonsBySubjectAndClass(UUID subjectId, UUID classGroupId);

    List<LessonResponse> getLessonsByClass(UUID classGroupId);

    ProgressResponse updateProgress(UUID institutionId, UUID studentId, ProgressRequest request);

    List<ProgressResponse> getStudentProgress(UUID studentId);

    Double getStudentAverageCompletion(UUID studentId);

    AssignmentResponse createAssignment(UUID institutionId, AssignmentRequest request);

    List<AssignmentResponse> getAssignmentsByClass(UUID classGroupId);

    SubmissionResponse submitAssignment(UUID assignmentId, UUID studentId, UUID institutionId);

    List<SubmissionResponse> getSubmissionsByAssignment(UUID assignmentId);

    SubmissionResponse gradeSubmission(UUID submissionId, Integer grade, String feedback, UUID gradedBy);
}
