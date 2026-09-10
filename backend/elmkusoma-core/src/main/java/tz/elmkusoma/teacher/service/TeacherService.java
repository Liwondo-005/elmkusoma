package tz.elmkusoma.teacher.service;

import tz.elmkusoma.common.PageResponse;
import tz.elmkusoma.teacher.dto.request.TeacherAssignmentRequest;
import tz.elmkusoma.teacher.dto.request.TeacherQualificationRequest;
import tz.elmkusoma.teacher.dto.request.TeacherRequest;
import tz.elmkusoma.teacher.dto.response.*;

import java.util.List;
import java.util.UUID;

public interface TeacherService {

    TeacherResponse createTeacher(UUID institutionId, TeacherRequest request);

    TeacherResponse getTeacher(UUID institutionId, UUID teacherId);

    PageResponse<TeacherResponse> listTeachers(UUID institutionId, int page, int size);

    TeacherResponse updateTeacher(UUID institutionId, UUID teacherId, TeacherRequest request);

    void deleteTeacher(UUID institutionId, UUID teacherId);

    TeacherAssignmentResponse addAssignment(UUID institutionId, UUID teacherId, TeacherAssignmentRequest request);

    List<TeacherAssignmentResponse> getAssignments(UUID institutionId, UUID teacherId);

    void removeAssignment(UUID institutionId, UUID assignmentId);

    TeacherQualificationResponse addQualification(UUID institutionId, UUID teacherId, TeacherQualificationRequest request);

    List<TeacherQualificationResponse> getQualifications(UUID institutionId, UUID teacherId);

    void removeQualification(UUID institutionId, UUID qualificationId);

    TeacherResponse getTeacherByUserId(UUID userId, UUID institutionId);

    List<TeacherClassResponse> getTeacherClasses(UUID userId, UUID institutionId);

    List<TeacherStudentResponse> getTeacherStudents(UUID userId, UUID institutionId);

    TeacherDashboardResponse getTeacherDashboard(UUID userId, UUID institutionId);
}
