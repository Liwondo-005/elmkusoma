package tz.elmkusoma.oversight.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tz.elmkusoma.shared.repository.InstitutionRepository;
import tz.elmkusoma.oversight.domain.District;
import tz.elmkusoma.oversight.domain.Region;
import tz.elmkusoma.oversight.dto.*;
import tz.elmkusoma.oversight.repository.DistrictRepository;
import tz.elmkusoma.oversight.repository.RegionRepository;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.domain.InstitutionMembership;
import tz.elmkusoma.shared.repository.InstitutionMembershipRepository;
import tz.elmkusoma.shared.repository.UserRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OversightService {

    private final RegionRepository regionRepository;
    private final DistrictRepository districtRepository;
    private final UserRepository userRepository;
    private final InstitutionRepository institutionRepository;
    private final InstitutionMembershipRepository membershipRepository;

    public OversightDashboardResponse getDashboard(UUID regionId, UUID districtId) {
        OversightDashboardResponse.OversightDashboardResponseBuilder builder = OversightDashboardResponse.builder();

        long totalInstitutions = institutionRepository.countByIsDeletedFalse();
        long totalUsers = userRepository.countByIsDeletedFalse();
        long totalTeachers = membershipRepository.countByRoleAndIsActiveTrue(InstitutionMembership.Role.TEACHER);
        long totalStudents = membershipRepository.countByRoleAndIsActiveTrue(InstitutionMembership.Role.STUDENT);
        long totalRegions = regionRepository.count();
        long totalDistricts = districtRepository.count();

        builder.totalInstitutions(totalInstitutions)
                .totalUsers(totalUsers)
                .totalTeachers(totalTeachers)
                .totalStudents(totalStudents)
                .totalRegions(totalRegions)
                .totalDistricts(totalDistricts)
                .activeLiveClasses(0L)
                .totalLessons(0L);

        List<Region> regions = regionRepository.findByIsDeletedFalseOrderByCreatedAtDesc();
        List<TopRegionStats> topRegions = regions.stream()
                .limit(10)
                .map(r -> {
                    long instCount = institutionRepository.countByRegionIdAndIsDeletedFalse(r.getId());
                    return TopRegionStats.builder()
                            .regionName(r.getName())
                            .regionCode(r.getCode())
                            .institutionCount(instCount)
                            .teacherCount(0L)
                            .studentCount(0L)
                            .build();
                })
                .collect(Collectors.toList());

        builder.topRegions(topRegions);
        builder.recentActivities(List.of());

        return builder.build();
    }

    public List<RegionResponse> getAllRegions() {
        return regionRepository.findByIsDeletedFalseOrderByCreatedAtDesc().stream()
                .map(r -> {
                    long instCount = institutionRepository.countByRegionIdAndIsDeletedFalse(r.getId());
                    long distCount = districtRepository.countByRegionIdAndIsDeletedFalse(r.getId());
                    return RegionResponse.builder()
                            .id(r.getId())
                            .name(r.getName())
                            .code(r.getCode())
                            .isActive(r.getIsActive())
                            .institutionCount(instCount)
                            .teacherCount(0L)
                            .studentCount(0L)
                            .build();
                })
                .collect(Collectors.toList());
    }

    public List<DistrictResponse> getDistrictsByRegion(UUID regionId) {
        Region region = regionRepository.findById(regionId)
                .orElseThrow(() -> new RuntimeException("Region not found"));
        return districtRepository.findByRegionIdAndIsDeletedFalse(regionId).stream()
                .map(d -> DistrictResponse.builder()
                        .id(d.getId())
                        .name(d.getName())
                        .code(d.getCode())
                        .regionId(d.getRegionId())
                        .regionName(region.getName())
                        .isActive(d.getIsActive())
                        .institutionCount(0L)
                        .teacherCount(0L)
                        .studentCount(0L)
                        .build())
                .collect(Collectors.toList());
    }

    public List<InstitutionSummary> getInstitutionsByRegion(UUID regionId) {
        return institutionRepository.findByRegionIdAndIsDeletedFalse(regionId).stream()
                .map(inst -> InstitutionSummary.builder()
                        .id(inst.getId())
                        .name(inst.getName())
                        .code(inst.getCode())
                        .type(inst.getType().name())
                        .isActive(inst.getIsActive())
                        .teacherCount(0L)
                        .studentCount(0L)
                        .build())
                .collect(Collectors.toList());
    }

    public List<InstitutionSummary> getInstitutionsByDistrict(UUID districtId) {
        return institutionRepository.findByDistrictIdAndIsDeletedFalse(districtId).stream()
                .map(inst -> InstitutionSummary.builder()
                        .id(inst.getId())
                        .name(inst.getName())
                        .code(inst.getCode())
                        .type(inst.getType().name())
                        .isActive(inst.getIsActive())
                        .teacherCount(0L)
                        .studentCount(0L)
                        .build())
                .collect(Collectors.toList());
    }
}
