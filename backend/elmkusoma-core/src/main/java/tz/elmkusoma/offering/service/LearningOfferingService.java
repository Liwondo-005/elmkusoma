package tz.elmkusoma.offering.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.academic.domain.EducationLevel;
import tz.elmkusoma.academic.domain.Subject;
import tz.elmkusoma.academic.repository.SubjectRepository;
import tz.elmkusoma.course.domain.Course;
import tz.elmkusoma.course.repository.CourseRepository;
import tz.elmkusoma.exception.ForbiddenException;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.learning.domain.Resource;
import tz.elmkusoma.offering.domain.LearningOffering;
import tz.elmkusoma.offering.dto.LearningOfferingRequest;
import tz.elmkusoma.offering.dto.LearningOfferingResponse;
import tz.elmkusoma.offering.repository.LearningOfferingRepository;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.teacher.domain.Teacher;
import tz.elmkusoma.teacher.domain.TeacherStatus;
import tz.elmkusoma.teacher.repository.TeacherRepository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LearningOfferingService {

    private static final Set<String> ALLOWED_STATUS = Set.of(
            LearningOffering.STATUS_DRAFT, LearningOffering.STATUS_PUBLISHED);
    private static final Set<Resource.ResourceVisibility> ALLOWED_VISIBILITY = Set.of(
            Resource.ResourceVisibility.PRIVATE,
            Resource.ResourceVisibility.INSTITUTION,
            Resource.ResourceVisibility.PUBLIC);

    private final LearningOfferingRepository offeringRepository;
    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;
    private final CourseRepository courseRepository;

    @Transactional
    public LearningOfferingResponse create(LearningOfferingRequest request, UUID userId, UUID institutionId) {
        boolean independent = Boolean.TRUE.equals(request.getIndependent()) || institutionId == null;

        LearningOffering offering = new LearningOffering();
        offering.setOwnerUserId(userId);
        offering.setInstitutionId(independent ? null : institutionId);
        offering.setTeacherId(resolveTeacherId(userId, independent ? null : institutionId));
        offering.setTitle(request.getTitle().trim());
        offering.setDescription(request.getDescription());
        offering.setThumbnailUrl(request.getThumbnailUrl());
        offering.setEducationLevel(request.getEducationLevel());
        offering.setSubjectId(validatedSubjectId(request.getSubjectId(), independent, institutionId));
        offering.setCourseId(validatedCourseId(request.getCourseId(), independent, institutionId));
        offering.setVisibility(resolveVisibility(request.getVisibility(), independent));
        offering.setStatus(resolveStatus(request.getStatus(), LearningOffering.STATUS_DRAFT));
        return toResponse(offeringRepository.save(offering));
    }

    @Transactional(readOnly = true)
    public List<LearningOfferingResponse> listMine(UUID userId) {
        return offeringRepository.findByOwnerUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public LearningOfferingResponse update(UUID id, LearningOfferingRequest request, UUID userId, UUID institutionId) {
        LearningOffering offering = requireNotDeleted(id);
        assertOwner(offering, userId);
        assertInstitutionScope(offering, institutionId);

        offering.setTitle(request.getTitle().trim());
        offering.setDescription(request.getDescription());
        offering.setThumbnailUrl(request.getThumbnailUrl());
        offering.setEducationLevel(request.getEducationLevel());
        offering.setSubjectId(validatedSubjectId(request.getSubjectId(),
                offering.getInstitutionId() == null, offering.getInstitutionId()));
        offering.setCourseId(validatedCourseId(request.getCourseId(),
                offering.getInstitutionId() == null, offering.getInstitutionId()));
        if (request.getVisibility() != null) {
            if (!ALLOWED_VISIBILITY.contains(request.getVisibility())) {
                throw new ForbiddenException("Unsupported visibility");
            }
            offering.setVisibility(request.getVisibility());
        }
        if (request.getStatus() != null) {
            offering.setStatus(resolveStatus(request.getStatus(), offering.getStatus()));
        }
        return toResponse(offeringRepository.save(offering));
    }

    @Transactional
    public void delete(UUID id, UUID userId, UUID institutionId) {
        LearningOffering offering = requireNotDeleted(id);
        assertOwner(offering, userId);
        assertInstitutionScope(offering, institutionId);
        offering.setIsDeleted(true);
        offeringRepository.save(offering);
    }

    @Transactional(readOnly = true)
    public Page<LearningOfferingResponse> discover(String q, EducationLevel educationLevel, UUID subjectId,
                                                   UUID ownerId, UUID userId, UUID institutionId, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 50);
        return offeringRepository.searchDiscoverable(
                        q == null ? "" : q.trim(), educationLevel, subjectId, ownerId, userId, institutionId,
                        Resource.ResourceVisibility.PUBLIC, Resource.ResourceVisibility.INSTITUTION,
                        PageRequest.of(Math.max(page, 0), safeSize))
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public LearningOfferingResponse getVisible(UUID id, UUID userId, UUID institutionId) {
        LearningOffering offering = requireNotDeleted(id);
        boolean visible = offering.getStatus().equals(LearningOffering.STATUS_PUBLISHED)
                && (offering.getVisibility() == Resource.ResourceVisibility.PUBLIC
                    || (offering.getVisibility() == Resource.ResourceVisibility.INSTITUTION
                        && institutionId != null
                        && institutionId.equals(offering.getInstitutionId()))
                    || offering.getOwnerUserId().equals(userId));
        if (!visible) {
            throw new ResourceNotFoundException("Learning offering", "id", id);
        }
        return toResponse(offering);
    }

    private LearningOffering requireNotDeleted(UUID id) {
        return offeringRepository.findById(id)
                .filter(o -> !Boolean.TRUE.equals(o.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Learning offering", "id", id));
    }

    private void assertOwner(LearningOffering offering, UUID userId) {
        if (!offering.getOwnerUserId().equals(userId)) {
            throw new ForbiddenException("You can only manage your own learning offerings");
        }
    }

    private void assertInstitutionScope(LearningOffering offering, UUID institutionId) {
        if (offering.getInstitutionId() != null
                && (institutionId == null || !offering.getInstitutionId().equals(institutionId))) {
            throw new ForbiddenException("Cross-institution learning offering access is not permitted");
        }
    }

    private UUID resolveTeacherId(UUID userId, UUID institutionId) {
        if (institutionId != null) {
            return teacherRepository.findByUserIdAndInstitutionId(userId, institutionId)
                    .orElseGet(() -> saveNewTeacher(userId, institutionId))
                    .getId();
        }
        return teacherRepository.findFirstByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId)
                .map(Teacher::getId)
                .orElseGet(() -> saveNewTeacher(userId, null).getId());
    }

    private Teacher saveNewTeacher(UUID userId, UUID institutionId) {
        User user = userRepository.findById(userId)
                .filter(u -> !Boolean.TRUE.equals(u.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        Teacher teacher = Teacher.builder()
                .userId(user.getId())
                .status(TeacherStatus.ACTIVE)
                .build();
        teacher.setInstitutionId(institutionId);
        return teacherRepository.save(teacher);
    }

    private UUID validatedSubjectId(UUID subjectId, boolean independent, UUID institutionId) {
        if (subjectId == null) {
            return null;
        }
        Subject subject = subjectRepository.findById(subjectId)
                .filter(s -> !Boolean.TRUE.equals(s.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Subject", "id", subjectId));
        if (!independent && institutionId != null
                && subject.getInstitutionId() != null
                && !institutionId.equals(subject.getInstitutionId())) {
            throw new ForbiddenException("Subject belongs to another institution");
        }
        return subject.getId();
    }

    private UUID validatedCourseId(UUID courseId, boolean independent, UUID institutionId) {
        if (courseId == null) {
            return null;
        }
        Course course = courseRepository.findById(courseId)
                .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));
        if (!independent && institutionId != null
                && course.getInstitutionId() != null
                && !institutionId.equals(course.getInstitutionId())) {
            throw new ForbiddenException("Course belongs to another institution");
        }
        return course.getId();
    }

    private Resource.ResourceVisibility resolveVisibility(Resource.ResourceVisibility requested, boolean independent) {
        if (independent) {
            return requested == null || requested == Resource.ResourceVisibility.INSTITUTION
                    ? Resource.ResourceVisibility.PUBLIC
                    : requested;
        }
        return requested == null ? Resource.ResourceVisibility.INSTITUTION : requested;
    }

    private String resolveStatus(String requested, String fallback) {
        if (requested == null) {
            return fallback;
        }
        String normalized = requested.trim().toUpperCase();
        if (!ALLOWED_STATUS.contains(normalized)) {
            throw new ForbiddenException("Unsupported offering status");
        }
        return normalized;
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private LearningOfferingResponse toResponse(LearningOffering offering) {
        String subjectName = null;
        if (offering.getSubjectId() != null) {
            subjectName = subjectRepository.findById(offering.getSubjectId())
                    .map(Subject::getName).orElse(null);
        }
        String courseTitle = null;
        if (offering.getCourseId() != null) {
            courseTitle = courseRepository.findById(offering.getCourseId())
                    .map(Course::getTitle).orElse(null);
        }
        String ownerName = userRepository.findById(offering.getOwnerUserId())
                .map(u -> joinName(u.getFirstName(), u.getLastName()))
                .orElse(null);
        return LearningOfferingResponse.builder()
                .id(offering.getId())
                .title(offering.getTitle())
                .description(offering.getDescription())
                .thumbnailUrl(offering.getThumbnailUrl())
                .educationLevel(offering.getEducationLevel())
                .visibility(offering.getVisibility())
                .status(offering.getStatus())
                .subjectId(offering.getSubjectId())
                .subjectName(subjectName)
                .courseId(offering.getCourseId())
                .courseTitle(courseTitle)
                .ownerId(offering.getOwnerUserId())
                .ownerName(ownerName)
                .teacherId(offering.getTeacherId())
                .institutionId(offering.getInstitutionId())
                .createdAt(offering.getCreatedAt())
                .build();
    }

    private String joinName(String firstName, String lastName) {
        String first = firstName == null ? "" : firstName.trim();
        String last = lastName == null ? "" : lastName.trim();
        String joined = (first + " " + last).trim();
        return joined.isEmpty() ? null : joined;
    }
}
