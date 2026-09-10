package tz.elmkusoma.parent.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.common.PageResponse;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.shared.security.OwnershipGuard;
import tz.elmkusoma.parent.domain.Parent;
import tz.elmkusoma.parent.domain.ParentNotificationPreference;
import tz.elmkusoma.parent.domain.ParentStudentLink;
import tz.elmkusoma.parent.dto.request.LinkStudentRequest;
import tz.elmkusoma.parent.dto.request.ParentNotificationPreferenceRequest;
import tz.elmkusoma.parent.dto.request.ParentRequest;
import tz.elmkusoma.parent.dto.response.ParentNotificationPreferenceResponse;
import tz.elmkusoma.parent.dto.response.ParentResponse;
import tz.elmkusoma.parent.dto.response.ParentStudentResponse;
import tz.elmkusoma.parent.repository.ParentNotificationPreferenceRepository;
import tz.elmkusoma.parent.repository.ParentRepository;
import tz.elmkusoma.parent.repository.ParentStudentLinkRepository;
import tz.elmkusoma.parent.service.ParentService;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.UserRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ParentServiceImpl implements ParentService {

    private final ParentRepository parentRepository;
    private final ParentStudentLinkRepository studentLinkRepository;
    private final ParentNotificationPreferenceRepository notificationPreferenceRepository;
    private final UserRepository userRepository;

    @Override
    public ParentResponse createParent(UUID institutionId, ParentRequest request) {
        UUID userId = UUID.fromString(request.getUserId());
        User user = userRepository.findById(userId)
                .filter(u -> !u.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (parentRepository.existsByUserIdAndInstitutionIdAndIsDeletedFalse(userId, institutionId)) {
            throw new IllegalArgumentException("Parent profile already exists for this user in this institution");
        }

        Parent.RelationshipType relType = Parent.RelationshipType.GUARDIAN;
        if (request.getRelationshipType() != null) {
            relType = Parent.RelationshipType.valueOf(request.getRelationshipType());
        }

        Parent parent = Parent.builder()
                .userId(userId)
                .occupation(request.getOccupation())
                .relationshipType(relType)
                .emergencyContact(request.getEmergencyContact())
                .build();
        parent.setInstitutionId(institutionId);

        Parent saved = parentRepository.save(parent);

        ParentNotificationPreference prefs = ParentNotificationPreference.builder()
                .parentId(saved.getId())
                .build();
        prefs.setInstitutionId(institutionId);
        notificationPreferenceRepository.save(prefs);

        return mapToResponse(saved, user);
    }

    @Override
    @Transactional(readOnly = true)
    public ParentResponse getParent(UUID institutionId, UUID parentId) {
        Parent parent = parentRepository.findByIdAndInstitutionId(parentId, institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent", "id", parentId));
        User user = userRepository.findById(parent.getUserId()).orElse(null);
        return mapToResponse(parent, user);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ParentResponse> listParents(UUID institutionId, int page, int size) {
        List<Parent> allParents = parentRepository.findAllByInstitutionId(institutionId);
        int start = Math.min(page * size, allParents.size());
        int end = Math.min((page + 1) * size, allParents.size());
        List<Parent> paged = allParents.subList(start, end);

        List<ParentResponse> content = paged.stream()
                .map(p -> {
                    User user = userRepository.findById(p.getUserId()).orElse(null);
                    return mapToResponse(p, user);
                })
                .toList();

        int totalPages = (int) Math.ceil((double) allParents.size() / size);

        return new PageResponse<>(
                content,
                page,
                size,
                allParents.size(),
                totalPages,
                page == 0,
                page >= totalPages - 1
        );
    }

    @Override
    public ParentResponse updateParent(UUID institutionId, UUID parentId, ParentRequest request) {
        Parent parent = parentRepository.findByIdAndInstitutionId(parentId, institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent", "id", parentId));

        if (request.getOccupation() != null) parent.setOccupation(request.getOccupation());
        if (request.getEmergencyContact() != null) parent.setEmergencyContact(request.getEmergencyContact());
        if (request.getRelationshipType() != null) {
            parent.setRelationshipType(Parent.RelationshipType.valueOf(request.getRelationshipType()));
        }

        Parent saved = parentRepository.save(parent);
        User user = userRepository.findById(saved.getUserId()).orElse(null);
        return mapToResponse(saved, user);
    }

    @Override
    public void deleteParent(UUID institutionId, UUID parentId) {
        Parent parent = parentRepository.findByIdAndInstitutionId(parentId, institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent", "id", parentId));
        parent.setIsDeleted(true);
        parentRepository.save(parent);
    }

    @Override
    public ParentStudentResponse linkStudent(UUID institutionId, UUID parentId, LinkStudentRequest request) {
        Parent parent = parentRepository.findByIdAndInstitutionId(parentId, institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent", "id", parentId));

        UUID studentId = UUID.fromString(request.getStudentId());

        if (studentLinkRepository.existsByParentIdAndStudentIdAndIsDeletedFalse(parentId, studentId)) {
            throw new IllegalArgumentException("Student is already linked to this parent");
        }

        Parent.RelationshipType relType = Parent.RelationshipType.GUARDIAN;
        if (request.getRelationshipType() != null) {
            relType = Parent.RelationshipType.valueOf(request.getRelationshipType());
        }

        ParentStudentLink link = ParentStudentLink.builder()
                .parentId(parentId)
                .studentId(studentId)
                .relationshipType(relType)
                .isPrimary(request.getIsPrimary() != null ? request.getIsPrimary() : false)
                .build();
        link.setInstitutionId(institutionId);

        ParentStudentLink saved = studentLinkRepository.save(link);
        return mapToStudentResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParentStudentResponse> getChildren(UUID institutionId, UUID parentId) {
        Parent parent = parentRepository.findByIdAndInstitutionId(parentId, institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent", "id", parentId));
        return studentLinkRepository.findAllByParentId(parentId).stream()
                .filter(l -> l.getInstitutionId().equals(institutionId))
                .map(this::mapToStudentResponse)
                .toList();
    }

    @Override
    public void unlinkStudent(UUID institutionId, UUID linkId) {
        ParentStudentLink link = studentLinkRepository.findById(linkId)
                .filter(l -> l.getInstitutionId().equals(institutionId))
                .orElseThrow(() -> new ResourceNotFoundException("Student link", "id", linkId));
        link.setIsDeleted(true);
        studentLinkRepository.save(link);
    }

    @Override
    @Transactional(readOnly = true)
    public ParentNotificationPreferenceResponse getNotificationPreferences(UUID institutionId, UUID parentId) {
        Parent parent = parentRepository.findByIdAndInstitutionId(parentId, institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent", "id", parentId));
        ParentNotificationPreference prefs = notificationPreferenceRepository.findByParentId(parentId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification preferences", "parentId", parentId));
        return mapToNotificationResponse(prefs);
    }

    @Override
    public ParentNotificationPreferenceResponse updateNotificationPreferences(UUID institutionId, UUID parentId, ParentNotificationPreferenceRequest request) {
        Parent parent = parentRepository.findByIdAndInstitutionId(parentId, institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent", "id", parentId));
        ParentNotificationPreference prefs = notificationPreferenceRepository.findByParentId(parentId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification preferences", "parentId", parentId));

        if (request.getAttendanceAlerts() != null) prefs.setAttendanceAlerts(request.getAttendanceAlerts());
        if (request.getGradeAlerts() != null) prefs.setGradeAlerts(request.getGradeAlerts());
        if (request.getFeeAlerts() != null) prefs.setFeeAlerts(request.getFeeAlerts());
        if (request.getGeneralAnnouncements() != null) prefs.setGeneralAnnouncements(request.getGeneralAnnouncements());
        if (request.getSmsEnabled() != null) prefs.setSmsEnabled(request.getSmsEnabled());
        if (request.getEmailEnabled() != null) prefs.setEmailEnabled(request.getEmailEnabled());
        if (request.getPushEnabled() != null) prefs.setPushEnabled(request.getPushEnabled());

        ParentNotificationPreference saved = notificationPreferenceRepository.save(prefs);
        return mapToNotificationResponse(saved);
    }

    private ParentResponse mapToResponse(Parent parent, User user) {
        return ParentResponse.builder()
                .id(parent.getId())
                .userId(parent.getUserId())
                .fullName(user != null ? user.getFullName() : null)
                .email(user != null ? user.getEmail() : null)
                .phone(user != null ? user.getPhone() : null)
                .occupation(parent.getOccupation())
                .relationshipType(parent.getRelationshipType().name())
                .emergencyContact(parent.getEmergencyContact())
                .createdAt(parent.getCreatedAt())
                .build();
    }

    private ParentStudentResponse mapToStudentResponse(ParentStudentLink link) {
        return ParentStudentResponse.builder()
                .id(link.getId())
                .parentId(link.getParentId())
                .studentId(link.getStudentId())
                .relationshipType(link.getRelationshipType().name())
                .isPrimary(link.getIsPrimary())
                .createdAt(link.getCreatedAt())
                .build();
    }

    private ParentNotificationPreferenceResponse mapToNotificationResponse(ParentNotificationPreference prefs) {
        return ParentNotificationPreferenceResponse.builder()
                .id(prefs.getId())
                .parentId(prefs.getParentId())
                .attendanceAlerts(prefs.getAttendanceAlerts())
                .gradeAlerts(prefs.getGradeAlerts())
                .feeAlerts(prefs.getFeeAlerts())
                .generalAnnouncements(prefs.getGeneralAnnouncements())
                .smsEnabled(prefs.getSmsEnabled())
                .emailEnabled(prefs.getEmailEnabled())
                .pushEnabled(prefs.getPushEnabled())
                .build();
    }
}
