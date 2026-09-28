package tz.elmkusoma.learning.service;

import tz.elmkusoma.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.learning.domain.Resource;
import tz.elmkusoma.learning.domain.ResourceAnnotation;
import tz.elmkusoma.learning.dto.ResourceAnnotationRequest;
import tz.elmkusoma.learning.dto.ResourceAnnotationResponse;
import tz.elmkusoma.learning.repository.ResourceAnnotationRepository;
import tz.elmkusoma.learning.repository.ResourceRepository;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.UserRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceAnnotationService {

    private final ResourceAnnotationRepository annotationRepository;
    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;

    @Transactional
    public ResourceAnnotationResponse createAnnotation(UUID resourceId, ResourceAnnotationRequest request,
                                                       UUID institutionId, UUID studentId) {
        Resource resource = resourceRepository.findById(resourceId)
                .filter(r -> institutionId == null || institutionId.equals(r.getInstitutionId()))
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));

        if (request.getParentAnnotationId() != null
                && !annotationRepository.existsById(request.getParentAnnotationId())) {
            throw new RuntimeException("Parent annotation not found");
        }

        ResourceAnnotation annotation = ResourceAnnotation.builder()
                .resourceId(resource.getId())
                .institutionId(resource.getInstitutionId() != null ? resource.getInstitutionId() : institutionId)
                .studentId(studentId)
                .content(request.getContent())
                .positionData(request.getPositionData())
                .isPrivate(request.getIsPrivate() == null || request.getIsPrivate())
                .parentAnnotationId(request.getParentAnnotationId())
                .createdBy(userRepository.findById(studentId).map(User::getEmail).orElse(null))
                .build();

        annotation = annotationRepository.save(annotation);
        log.info("Annotation {} created for resource {} by student {}", annotation.getId(), resourceId, studentId);
        return toResponse(annotation, Map.of());
    }

    @Transactional(readOnly = true)
    public List<ResourceAnnotationResponse> listAnnotations(UUID resourceId, UUID institutionId, String userRole) {
        resourceRepository.findById(resourceId)
                .filter(r -> institutionId == null || institutionId.equals(r.getInstitutionId()))
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));

        List<ResourceAnnotation> annotations =
                annotationRepository.findByResourceIdAndIsDeletedFalseOrderByCreatedAtDesc(resourceId);
        Map<UUID, String> names = resolveStudentNames(annotations);
        return annotations.stream().map(a -> toResponse(a, names)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ResourceAnnotationResponse> listMyAnnotations(UUID studentId) {
        List<ResourceAnnotation> annotations = annotationRepository.findByStudentIdAndIsDeletedFalse(studentId);
        Map<UUID, String> names = resolveStudentNames(annotations);
        return annotations.stream().map(a -> toResponse(a, names)).collect(Collectors.toList());
    }

    @Transactional
    public void deleteAnnotation(UUID annotationId, UUID institutionId, UUID actorId) {
        ResourceAnnotation annotation = annotationRepository.findById(annotationId)
                .orElseThrow(() -> new RuntimeException("Annotation not found"));
        boolean isOwner = actorId != null && actorId.equals(annotation.getStudentId());
        if (!isOwner) {
            throw new RuntimeException("You can only delete your own annotations");
        }
        annotation.setIsDeleted(true);
        annotationRepository.save(annotation);
    }

    private Map<UUID, String> resolveStudentNames(List<ResourceAnnotation> annotations) {
        return annotations.stream()
                .map(ResourceAnnotation::getStudentId)
                .distinct()
                .collect(Collectors.toMap(id -> id,
                        id -> userRepository.findById(id)
                                .map(u -> u.getFullName() != null ? u.getFullName() : u.getEmail())
                                .orElse("Unknown"),
                        (a, b) -> a));
    }

    private ResourceAnnotationResponse toResponse(ResourceAnnotation a, Map<UUID, String> names) {
        return ResourceAnnotationResponse.builder()
                .id(a.getId())
                .resourceId(a.getResourceId())
                .studentId(a.getStudentId())
                .studentName(names.getOrDefault(a.getStudentId(), "Unknown"))
                .content(a.getContent())
                .positionData(a.getPositionData())
                .isPrivate(a.getIsPrivate())
                .parentAnnotationId(a.getParentAnnotationId())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }
}
