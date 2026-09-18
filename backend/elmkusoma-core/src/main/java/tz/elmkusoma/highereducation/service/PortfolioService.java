package tz.elmkusoma.highereducation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.highereducation.domain.Portfolio;
import tz.elmkusoma.highereducation.domain.PortfolioItem;
import tz.elmkusoma.highereducation.dto.PortfolioDTO;
import tz.elmkusoma.highereducation.dto.PortfolioItemDTO;
import tz.elmkusoma.highereducation.repository.PortfolioItemRepository;
import tz.elmkusoma.highereducation.repository.PortfolioRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final PortfolioItemRepository portfolioItemRepository;

    // ── Portfolio CRUD ───────────────────────────────────────────

    public PortfolioDTO createPortfolio(PortfolioDTO dto) {
        Portfolio portfolio = Portfolio.builder()
                .institutionId(dto.getInstitutionId())
                .studentId(dto.getStudentId())
                .title(dto.getTitle())
                .visibility(dto.getVisibility() != null ? dto.getVisibility() : tz.elmkusoma.highereducation.domain.Visibility.PRIVATE)
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .build();
        Portfolio saved = portfolioRepository.save(portfolio);
        return toDTO(saved, new ArrayList<>());
    }

    @Transactional(readOnly = true)
    public List<PortfolioDTO> getPortfolios(UUID institutionId) {
        return portfolioRepository.findByInstitutionIdAndIsDeletedFalse(institutionId)
                .stream()
                .map(p -> {
                    List<PortfolioItem> items = portfolioItemRepository.findByPortfolioIdAndIsDeletedFalse(p.getId());
                    return toDTO(p, items);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PortfolioDTO getPortfolio(UUID id) {
        Portfolio portfolio = portfolioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio", "id", id));
        List<PortfolioItem> items = portfolioItemRepository.findByPortfolioIdAndIsDeletedFalse(id);
        return toDTO(portfolio, items);
    }

    public PortfolioDTO updatePortfolio(UUID id, PortfolioDTO dto) {
        Portfolio portfolio = portfolioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio", "id", id));
        portfolio.setStudentId(dto.getStudentId());
        portfolio.setTitle(dto.getTitle());
        portfolio.setVisibility(dto.getVisibility());
        portfolio.setIsActive(dto.getIsActive());
        Portfolio saved = portfolioRepository.save(portfolio);
        List<PortfolioItem> items = portfolioItemRepository.findByPortfolioIdAndIsDeletedFalse(id);
        return toDTO(saved, items);
    }

    public void deletePortfolio(UUID id) {
        Portfolio portfolio = portfolioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio", "id", id));
        portfolio.setIsDeleted(true);
        portfolioRepository.save(portfolio);
    }

    @Transactional(readOnly = true)
    public PortfolioDTO getStudentPortfolio(UUID studentId) {
        List<Portfolio> portfolios = portfolioRepository.findByStudentIdAndIsDeletedFalse(studentId);
        if (portfolios.isEmpty()) {
            throw new ResourceNotFoundException("Portfolio", "studentId", studentId);
        }
        Portfolio portfolio = portfolios.get(0);
        List<PortfolioItem> items = portfolioItemRepository.findByPortfolioIdAndIsDeletedFalse(portfolio.getId());
        return toDTO(portfolio, items);
    }

    @Transactional(readOnly = true)
    public PortfolioDTO getPublicPortfolio(UUID studentId) {
        List<Portfolio> portfolios = portfolioRepository.findByStudentIdAndIsDeletedFalse(studentId);
        if (portfolios.isEmpty()) {
            throw new ResourceNotFoundException("Portfolio", "studentId", studentId);
        }
        Portfolio portfolio = portfolios.get(0);
        List<PortfolioItem> items = portfolioItemRepository.findByPortfolioIdAndIsDeletedFalse(portfolio.getId())
                .stream()
                .filter(PortfolioItem::getIsVisible)
                .collect(Collectors.toList());
        return toDTO(portfolio, items);
    }

    // ── Portfolio Item CRUD ──────────────────────────────────────

    public PortfolioItemDTO addItem(UUID portfolioId, PortfolioItemDTO dto) {
        portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio", "id", portfolioId));
        PortfolioItem item = PortfolioItem.builder()
                .portfolioId(portfolioId)
                .institutionId(dto.getInstitutionId())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .itemType(dto.getItemType())
                .fileUrl(dto.getFileUrl())
                .competencyId(dto.getCompetencyId())
                .projectId(dto.getProjectId())
                .dateObtained(dto.getDateObtained())
                .sortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0)
                .isVisible(dto.getIsVisible() != null ? dto.getIsVisible() : true)
                .build();
        PortfolioItem saved = portfolioItemRepository.save(item);
        return toItemDTO(saved);
    }

    public PortfolioItemDTO updateItem(UUID itemId, PortfolioItemDTO dto) {
        PortfolioItem item = portfolioItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("PortfolioItem", "id", itemId));
        item.setTitle(dto.getTitle());
        item.setDescription(dto.getDescription());
        item.setItemType(dto.getItemType());
        item.setFileUrl(dto.getFileUrl());
        item.setCompetencyId(dto.getCompetencyId());
        item.setProjectId(dto.getProjectId());
        item.setDateObtained(dto.getDateObtained());
        item.setSortOrder(dto.getSortOrder());
        item.setIsVisible(dto.getIsVisible());
        PortfolioItem saved = portfolioItemRepository.save(item);
        return toItemDTO(saved);
    }

    public void deleteItem(UUID itemId) {
        PortfolioItem item = portfolioItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("PortfolioItem", "id", itemId));
        item.setIsDeleted(true);
        portfolioItemRepository.save(item);
    }

    @Transactional(readOnly = true)
    public List<PortfolioItemDTO> getItems(UUID portfolioId) {
        return portfolioItemRepository.findByPortfolioIdAndIsDeletedFalse(portfolioId)
                .stream()
                .map(this::toItemDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PortfolioItemDTO> getItemsByType(UUID portfolioId, tz.elmkusoma.highereducation.domain.PortfolioItemType itemType) {
        return portfolioItemRepository.findByPortfolioIdAndItemTypeAndIsDeletedFalse(portfolioId, itemType)
                .stream()
                .map(this::toItemDTO)
                .collect(Collectors.toList());
    }

    // ── Mapping helpers ──────────────────────────────────────────

    private PortfolioDTO toDTO(Portfolio portfolio, List<PortfolioItem> items) {
        List<PortfolioItemDTO> itemDTOs = items.stream()
                .map(this::toItemDTO)
                .collect(Collectors.toList());
        return PortfolioDTO.builder()
                .id(portfolio.getId())
                .institutionId(portfolio.getInstitutionId())
                .studentId(portfolio.getStudentId())
                .title(portfolio.getTitle())
                .visibility(portfolio.getVisibility())
                .isActive(portfolio.getIsActive())
                .items(itemDTOs)
                .build();
    }

    private PortfolioItemDTO toItemDTO(PortfolioItem item) {
        return PortfolioItemDTO.builder()
                .id(item.getId())
                .portfolioId(item.getPortfolioId())
                .institutionId(item.getInstitutionId())
                .title(item.getTitle())
                .description(item.getDescription())
                .itemType(item.getItemType())
                .fileUrl(item.getFileUrl())
                .competencyId(item.getCompetencyId())
                .projectId(item.getProjectId())
                .dateObtained(item.getDateObtained())
                .sortOrder(item.getSortOrder())
                .isVisible(item.getIsVisible())
                .build();
    }
}
