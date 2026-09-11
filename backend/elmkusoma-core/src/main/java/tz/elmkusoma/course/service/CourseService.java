package tz.elmkusoma.course.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.course.domain.Course;
import tz.elmkusoma.course.domain.CourseLesson;
import tz.elmkusoma.course.domain.CourseModule;
import tz.elmkusoma.course.domain.LiveClass;
import tz.elmkusoma.course.dto.*;
import tz.elmkusoma.course.mapper.CourseMapper;
import tz.elmkusoma.course.repository.*;
import tz.elmkusoma.academic.domain.Subject;
import tz.elmkusoma.academic.repository.SubjectRepository;
import tz.elmkusoma.exception.ForbiddenException;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.UserRepository;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CourseService {

    private final CourseRepository courseRepository;
    private final CourseModuleRepository moduleRepository;
    private final CourseLessonRepository lessonRepository;
    private final LiveClassRepository liveClassRepository;
    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;
    private final CourseMapper courseMapper;

    // ── Course CRUD ──

    public CourseResponse createCourse(CourseRequest request, UUID institutionId, UUID userId) {
        if (courseRepository.existsByInstitutionIdAndTitleAndIsDeletedFalse(institutionId, request.getTitle())) {
            throw new IllegalArgumentException("Course with title '" + request.getTitle() + "' already exists");
        }

        Course course = Course.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .subjectId(request.getSubjectId())
                .level(request.getLevel() != null ? request.getLevel() : "ALL_LEVELS")
                .category(request.getCategory())
                .thumbnailUrl(request.getThumbnailUrl())
                .isPublished(request.getIsPublished() != null ? request.getIsPublished() : false)
                .isFeatured(request.getIsFeatured() != null ? request.getIsFeatured() : false)
                .build();
        course.setInstitutionId(institutionId);

        courseRepository.save(course);
        log.info("Created course: {} for institution: {}", course.getTitle(), institutionId);

        return courseMapper.toCourseResponse(course, null, 0L, 0L, course.getCreatedBy());
    }

    @Transactional(readOnly = true)
    public List<CourseResponse> getCourses(UUID institutionId) {
        List<Course> courses = courseRepository.findAllByInstitutionIdOrderByCreatedAtDesc(institutionId);
        return courses.stream()
                .map(c -> {
                    long modules = moduleRepository.countByCourseIdAndIsDeletedFalse(c.getId());
                    long lessons = countLessonsForCourse(c.getId());
                    String subjectName = c.getSubjectId() != null ? resolveSubjectName(c.getSubjectId()) : null;
                    return courseMapper.toCourseResponse(c, subjectName, modules, lessons, c.getCreatedBy());
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public CourseResponse getCourseById(UUID courseId, UUID institutionId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));

        if (!course.getInstitutionId().equals(institutionId)) {
            throw new ForbiddenException("course", "access");
        }

        long modules = moduleRepository.countByCourseIdAndIsDeletedFalse(courseId);
        long lessons = countLessonsForCourse(courseId);
        String subjectName = course.getSubjectId() != null ? resolveSubjectName(course.getSubjectId()) : null;

        return courseMapper.toCourseResponse(course, subjectName, modules, lessons, course.getCreatedBy());
    }

    public CourseResponse updateCourse(UUID courseId, CourseRequest request, UUID institutionId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));

        if (!course.getInstitutionId().equals(institutionId)) {
            throw new ForbiddenException("course", "update");
        }

        if (request.getTitle() != null) course.setTitle(request.getTitle());
        if (request.getDescription() != null) course.setDescription(request.getDescription());
        if (request.getSubjectId() != null) course.setSubjectId(request.getSubjectId());
        if (request.getLevel() != null) course.setLevel(request.getLevel());
        if (request.getCategory() != null) course.setCategory(request.getCategory());
        if (request.getThumbnailUrl() != null) course.setThumbnailUrl(request.getThumbnailUrl());
        if (request.getIsPublished() != null) course.setIsPublished(request.getIsPublished());
        if (request.getIsFeatured() != null) course.setIsFeatured(request.getIsFeatured());

        courseRepository.save(course);
        log.info("Updated course: {} in institution: {}", courseId, institutionId);

        long modules = moduleRepository.countByCourseIdAndIsDeletedFalse(courseId);
        long lessons = countLessonsForCourse(courseId);
        String subjectName = course.getSubjectId() != null ? resolveSubjectName(course.getSubjectId()) : null;

        return courseMapper.toCourseResponse(course, subjectName, modules, lessons, course.getCreatedBy());
    }

    public void deleteCourse(UUID courseId, UUID institutionId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));

        if (!course.getInstitutionId().equals(institutionId)) {
            throw new ForbiddenException("course", "delete");
        }

        course.setIsDeleted(true);
        courseRepository.save(course);
        log.info("Deleted course: {} from institution: {}", courseId, institutionId);
    }

    public CourseResponse togglePublish(UUID courseId, UUID institutionId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));

        if (!course.getInstitutionId().equals(institutionId)) {
            throw new ForbiddenException("course", "update");
        }

        course.setIsPublished(!course.getIsPublished());
        courseRepository.save(course);

        long modules = moduleRepository.countByCourseIdAndIsDeletedFalse(courseId);
        long lessons = countLessonsForCourse(courseId);
        String subjectName = course.getSubjectId() != null ? resolveSubjectName(course.getSubjectId()) : null;

        return courseMapper.toCourseResponse(course, subjectName, modules, lessons, course.getCreatedBy());
    }

    // ── Module CRUD ──

    public CourseModuleResponse createModule(CourseModuleRequest request, UUID courseId, UUID institutionId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));

        if (!course.getInstitutionId().equals(institutionId)) {
            throw new ForbiddenException("course", "add module to");
        }

        CourseModule module = CourseModule.builder()
                .courseId(courseId)
                .title(request.getTitle())
                .description(request.getDescription())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .build();
        module.setInstitutionId(institutionId);

        moduleRepository.save(module);
        return courseMapper.toModuleResponse(module, 0L);
    }

    @Transactional(readOnly = true)
    public List<CourseModuleResponse> getModules(UUID courseId) {
        List<CourseModule> modules = moduleRepository.findByCourseIdAndIsDeletedFalseOrderBySortOrder(courseId);
        return modules.stream()
                .map(m -> {
                    long lessons = lessonRepository.countByModuleIdAndIsDeletedFalse(m.getId());
                    return courseMapper.toModuleResponse(m, lessons);
                })
                .toList();
    }

    public void deleteModule(UUID moduleId, UUID institutionId) {
        CourseModule module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException("CourseModule", "id", moduleId));

        module.setIsDeleted(true);
        moduleRepository.save(module);
        log.info("Deleted module: {} from institution: {}", moduleId, institutionId);
    }

    // ── Lesson CRUD ──

    public CourseLessonResponse createLesson(CourseLessonRequest request, UUID moduleId, UUID institutionId) {
        CourseLesson lesson = CourseLesson.builder()
                .moduleId(moduleId)
                .title(request.getTitle())
                .contentType(request.getContentType())
                .contentUrl(request.getContentUrl())
                .durationMinutes(request.getDurationMinutes())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .isFree(request.getIsFree() != null ? request.getIsFree() : false)
                .build();
        lesson.setInstitutionId(institutionId);

        lessonRepository.save(lesson);
        return courseMapper.toLessonResponse(lesson);
    }

    @Transactional(readOnly = true)
    public List<CourseLessonResponse> getLessons(UUID moduleId) {
        List<CourseLesson> lessons = lessonRepository.findByModuleIdAndIsDeletedFalseOrderBySortOrder(moduleId);
        return lessons.stream()
                .map(courseMapper::toLessonResponse)
                .toList();
    }

    public void deleteLesson(UUID lessonId, UUID institutionId) {
        CourseLesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("CourseLesson", "id", lessonId));

        lesson.setIsDeleted(true);
        lessonRepository.save(lesson);
        log.info("Deleted lesson: {} from institution: {}", lessonId, institutionId);
    }

    // ── Stats ──

    @Transactional(readOnly = true)
    public CourseStatsResponse getCourseStats(UUID institutionId) {
        long totalCourses = courseRepository.countByInstitutionIdAndIsDeletedFalse(institutionId);
        long publishedCourses = courseRepository.countByInstitutionIdAndIsPublishedAndIsDeletedFalse(institutionId, true);
        long draftCourses = totalCourses - publishedCourses;
        long featuredCourses = courseRepository.countByInstitutionIdAndIsFeaturedAndIsDeletedFalse(institutionId, true);

        long totalModules = 0;
        long totalLessons = 0;
        List<Course> courses = courseRepository.findByInstitutionIdAndIsDeletedFalse(institutionId);
        for (Course c : courses) {
            totalModules += moduleRepository.countByCourseIdAndIsDeletedFalse(c.getId());
            totalLessons += countLessonsForCourse(c.getId());
        }

        long liveScheduled = liveClassRepository.countByInstitutionIdAndStatusAndIsDeletedFalse(institutionId, "SCHEDULED");
        long liveCompleted = liveClassRepository.countByInstitutionIdAndStatusAndIsDeletedFalse(institutionId, "COMPLETED");

        Map<String, Long> byLevel = new LinkedHashMap<>();
        for (String level : List.of("NURSERY", "PRIMARY", "SECONDARY", "COLLEGE", "VETA", "UNIVERSITY", "ALL_LEVELS")) {
            long count = courseRepository.countByInstitutionIdAndLevelAndIsDeletedFalse(institutionId, level);
            if (count > 0) byLevel.put(level, count);
        }

        return CourseStatsResponse.builder()
                .totalCourses(totalCourses)
                .publishedCourses(publishedCourses)
                .draftCourses(draftCourses)
                .featuredCourses(featuredCourses)
                .totalModules(totalModules)
                .totalLessons(totalLessons)
                .liveClassesScheduled(liveScheduled)
                .liveClassesCompleted(liveCompleted)
                .coursesByLevel(byLevel)
                .build();
    }

    // ── Helpers ──

    private long countLessonsForCourse(UUID courseId) {
        List<CourseModule> modules = moduleRepository.findByCourseIdAndIsDeletedFalseOrderBySortOrder(courseId);
        long total = 0;
        for (CourseModule m : modules) {
            total += lessonRepository.countByModuleIdAndIsDeletedFalse(m.getId());
        }
        return total;
    }

    private String resolveUserName(UUID userId) {
        return userRepository.findById(userId)
                .map(u -> u.getFirstName() + " " + u.getLastName())
                .orElse("Unknown");
    }

    private String resolveSubjectName(UUID subjectId) {
        return subjectRepository.findById(subjectId)
                .map(Subject::getName)
                .orElse(null);
    }
}
