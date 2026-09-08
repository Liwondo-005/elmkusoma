package tz.elmkusoma.institution.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.common.PageResponse;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.institution.dto.request.CreateInstitutionRequest;
import tz.elmkusoma.institution.dto.request.UpdateInstitutionRequest;
import tz.elmkusoma.institution.dto.response.InstitutionResponse;
import tz.elmkusoma.institution.service.InstitutionService;
import tz.elmkusoma.shared.domain.Institution;
import tz.elmkusoma.shared.domain.InstitutionMembership;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.InstitutionMembershipRepository;
import tz.elmkusoma.shared.repository.InstitutionRepository;
import tz.elmkusoma.shared.repository.UserRepository;

import java.util.UUID;

@Service
@Transactional
public class InstitutionServiceImpl implements InstitutionService {

    private static final Logger log = LoggerFactory.getLogger(InstitutionServiceImpl.class);

    private final InstitutionRepository institutionRepository;
    private final InstitutionMembershipRepository membershipRepository;
    private final UserRepository userRepository;

    public InstitutionServiceImpl(InstitutionRepository institutionRepository,
                                  InstitutionMembershipRepository membershipRepository,
                                  UserRepository userRepository) {
        this.institutionRepository = institutionRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
    }

    @Override
    public InstitutionResponse createInstitution(CreateInstitutionRequest request, UUID ownerUserId) {
        Institution.InstitutionType type;
        try {
            type = Institution.InstitutionType.valueOf(request.getType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid institution type: " + request.getType());
        }

        String code = generateCode(request.getName());

        Institution institution = Institution.builder()
                .name(request.getName())
                .code(code)
                .type(type)
                .description(request.getDescription())
                .logoUrl(request.getLogoUrl())
                .website(request.getWebsite())
                .email(request.getEmail())
                .phone(request.getPhone())
                .address(request.getAddress())
                .city(request.getCity())
                .country(request.getCountry())
                .isActive(true)
                .build();

        institution = institutionRepository.save(institution);
        log.info("Institution created: {} (ID: {})", institution.getName(), institution.getId());

        User owner = userRepository.findById(ownerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", ownerUserId));

        InstitutionMembership membership = InstitutionMembership.builder()
                .userId(ownerUserId)
                .institutionId(institution.getId())
                .role(InstitutionMembership.Role.OWNER)
                .isActive(true)
                .build();
        membershipRepository.save(membership);

        return mapToResponse(institution);
    }

    @Override
    @Transactional(readOnly = true)
    public InstitutionResponse getInstitution(UUID id) {
        Institution institution = institutionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Institution", "id", id));
        return mapToResponse(institution);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<InstitutionResponse> listInstitutions(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Institution> institutionPage = institutionRepository.findByIsActiveTrueAndIsDeletedFalse(pageable);

        return new PageResponse<>(
                institutionPage.getContent().stream().map(this::mapToResponse).toList(),
                institutionPage.getNumber(),
                institutionPage.getSize(),
                institutionPage.getTotalElements(),
                institutionPage.getTotalPages(),
                institutionPage.isFirst(),
                institutionPage.isLast()
        );
    }

    @Override
    public InstitutionResponse updateInstitution(UUID id, UpdateInstitutionRequest request) {
        Institution institution = institutionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Institution", "id", id));

        if (request.getName() != null) institution.setName(request.getName());
        if (request.getDescription() != null) institution.setDescription(request.getDescription());
        if (request.getLogoUrl() != null) institution.setLogoUrl(request.getLogoUrl());
        if (request.getWebsite() != null) institution.setWebsite(request.getWebsite());
        if (request.getEmail() != null) institution.setEmail(request.getEmail());
        if (request.getPhone() != null) institution.setPhone(request.getPhone());
        if (request.getAddress() != null) institution.setAddress(request.getAddress());
        if (request.getCity() != null) institution.setCity(request.getCity());
        if (request.getCountry() != null) institution.setCountry(request.getCountry());

        institution = institutionRepository.save(institution);
        log.info("Institution updated: {} (ID: {})", institution.getName(), institution.getId());

        return mapToResponse(institution);
    }

    @Override
    public void deleteInstitution(UUID id) {
        Institution institution = institutionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Institution", "id", id));
        institution.setIsDeleted(true);
        institutionRepository.save(institution);
        log.info("Institution soft-deleted: {} (ID: {})", institution.getName(), institution.getId());
    }

    @Override
    public InstitutionResponse activateInstitution(UUID id) {
        Institution institution = institutionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Institution", "id", id));
        institution.setIsActive(true);
        institution = institutionRepository.save(institution);
        log.info("Institution activated: {} (ID: {})", institution.getName(), institution.getId());
        return mapToResponse(institution);
    }

    @Override
    public InstitutionResponse deactivateInstitution(UUID id) {
        Institution institution = institutionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Institution", "id", id));
        institution.setIsActive(false);
        institution = institutionRepository.save(institution);
        log.info("Institution deactivated: {} (ID: {})", institution.getName(), institution.getId());
        return mapToResponse(institution);
    }

    private String generateCode(String name) {
        String base = name.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
        if (base.length() > 10) base = base.substring(0, 10);
        return base + System.currentTimeMillis() % 10000;
    }

    private InstitutionResponse mapToResponse(Institution institution) {
        return InstitutionResponse.builder()
                .id(institution.getId())
                .name(institution.getName())
                .description(institution.getDescription())
                .type(institution.getType() != null ? institution.getType().name() : null)
                .status(institution.getIsActive() ? "ACTIVE" : "INACTIVE")
                .logoUrl(institution.getLogoUrl())
                .website(institution.getWebsite())
                .email(institution.getEmail())
                .phone(institution.getPhone())
                .address(institution.getAddress())
                .city(institution.getCity())
                .country(institution.getCountry())
                .createdAt(institution.getCreatedAt())
                .build();
    }
}
