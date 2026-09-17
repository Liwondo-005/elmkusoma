package tz.elmkusoma.oversight.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.oversight.dto.*;
import tz.elmkusoma.oversight.service.OversightService;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.UserRepository;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/oversight")
@RequiredArgsConstructor
public class OversightController {

    private final OversightService oversightService;
    private final UserRepository userRepository;

    private UUID getUserRegionId(UUID userId) {
        return userRepository.findById(userId)
                .map(User::getRegionId)
                .orElse(null);
    }

    private UUID getUserDistrictId(UUID userId) {
        return userRepository.findById(userId)
                .map(User::getDistrictId)
                .orElse(null);
    }

    private String getUserRole(UUID userId) {
        return userRepository.findById(userId)
                .map(u -> u.getRole().name())
                .orElse(null);
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('NATIONAL_ADMIN', 'REGIONAL_ADMIN', 'DISTRICT_ADMIN')")
    public ResponseEntity<OversightDashboardResponse> getDashboard(
            @RequestAttribute("userId") UUID userId) {
        String role = getUserRole(userId);
        UUID regionId = null;
        UUID districtId = null;

        if ("REGIONAL_ADMIN".equals(role)) {
            regionId = getUserRegionId(userId);
        } else if ("DISTRICT_ADMIN".equals(role)) {
            districtId = getUserDistrictId(userId);
            if (districtId != null) {
                regionId = getUserRegionId(userId);
            }
        }

        return ResponseEntity.ok(oversightService.getDashboard(regionId, districtId));
    }

    @GetMapping("/regions")
    @PreAuthorize("hasAnyRole('NATIONAL_ADMIN', 'REGIONAL_ADMIN')")
    public ResponseEntity<List<RegionResponse>> getRegions() {
        return ResponseEntity.ok(oversightService.getAllRegions());
    }

    @GetMapping("/regions/{regionId}/districts")
    @PreAuthorize("hasAnyRole('NATIONAL_ADMIN', 'REGIONAL_ADMIN')")
    public ResponseEntity<List<DistrictResponse>> getDistricts(@PathVariable UUID regionId) {
        return ResponseEntity.ok(oversightService.getDistrictsByRegion(regionId));
    }

    @GetMapping("/regions/{regionId}/institutions")
    @PreAuthorize("hasAnyRole('NATIONAL_ADMIN', 'REGIONAL_ADMIN')")
    public ResponseEntity<List<InstitutionSummary>> getInstitutionsByRegion(@PathVariable UUID regionId) {
        return ResponseEntity.ok(oversightService.getInstitutionsByRegion(regionId));
    }

    @GetMapping("/districts/{districtId}/institutions")
    @PreAuthorize("hasAnyRole('NATIONAL_ADMIN', 'REGIONAL_ADMIN', 'DISTRICT_ADMIN')")
    public ResponseEntity<List<InstitutionSummary>> getInstitutionsByDistrict(@PathVariable UUID districtId) {
        return ResponseEntity.ok(oversightService.getInstitutionsByDistrict(districtId));
    }

    @GetMapping("/institutions/{institutionId}")
    @PreAuthorize("hasAnyRole('NATIONAL_ADMIN', 'REGIONAL_ADMIN', 'DISTRICT_ADMIN')")
    public ResponseEntity<InstitutionDetailResponse> getInstitutionDetail(
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID institutionId) {
        String role = getUserRole(userId);
        UUID userRegionId = getUserRegionId(userId);
        UUID userDistrictId = getUserDistrictId(userId);

        if ("REGIONAL_ADMIN".equals(role) && userRegionId != null) {
            // Verify institution is in user's region
            // TODO: add service method to verify
        } else if ("DISTRICT_ADMIN".equals(role) && userDistrictId != null) {
            // Verify institution is in user's district
        }

        return ResponseEntity.ok(oversightService.getInstitutionDetail(institutionId));
    }

    @GetMapping("/schools")
    @PreAuthorize("hasAnyRole('NATIONAL_ADMIN', 'REGIONAL_ADMIN', 'DISTRICT_ADMIN')")
    public ResponseEntity<List<InstitutionSummary>> getSchools(
            @RequestAttribute("userId") UUID userId) {
        String role = getUserRole(userId);
        UUID regionId = null;
        UUID districtId = null;

        if ("REGIONAL_ADMIN".equals(role)) {
            regionId = getUserRegionId(userId);
        } else if ("DISTRICT_ADMIN".equals(role)) {
            districtId = getUserDistrictId(userId);
            if (districtId != null) {
                regionId = getUserRegionId(userId);
            }
        }

        List<InstitutionSummary> schools;
        if (districtId != null) {
            schools = oversightService.getInstitutionsByDistrict(districtId);
        } else if (regionId != null) {
            schools = oversightService.getInstitutionsByRegion(regionId);
        } else {
            schools = oversightService.getInstitutionsByRegion(null);
        }

        return ResponseEntity.ok(schools);
    }

    @GetMapping("/performance")
    @PreAuthorize("hasAnyRole('NATIONAL_ADMIN', 'REGIONAL_ADMIN', 'DISTRICT_ADMIN')")
    public ResponseEntity<PerformanceResponse> getPerformance(
            @RequestAttribute("userId") UUID userId) {
        String role = getUserRole(userId);
        UUID regionId = null;
        UUID districtId = null;

        if ("REGIONAL_ADMIN".equals(role)) {
            regionId = getUserRegionId(userId);
        } else if ("DISTRICT_ADMIN".equals(role)) {
            districtId = getUserDistrictId(userId);
            if (districtId != null) {
                regionId = getUserRegionId(userId);
            }
        }

        return ResponseEntity.ok(oversightService.getPerformance(regionId, districtId));
    }

    @GetMapping("/attendance")
    @PreAuthorize("hasAnyRole('NATIONAL_ADMIN', 'REGIONAL_ADMIN', 'DISTRICT_ADMIN')")
    public ResponseEntity<AttendanceResponse> getAttendance(
            @RequestAttribute("userId") UUID userId) {
        String role = getUserRole(userId);
        UUID regionId = null;
        UUID districtId = null;

        if ("REGIONAL_ADMIN".equals(role)) {
            regionId = getUserRegionId(userId);
        } else if ("DISTRICT_ADMIN".equals(role)) {
            districtId = getUserDistrictId(userId);
            if (districtId != null) {
                regionId = getUserRegionId(userId);
            }
        }

        return ResponseEntity.ok(oversightService.getAttendance(regionId, districtId));
    }

    @GetMapping("/curriculum")
    @PreAuthorize("hasAnyRole('NATIONAL_ADMIN', 'REGIONAL_ADMIN', 'DISTRICT_ADMIN')")
    public ResponseEntity<CurriculumResponse> getCurriculum(
            @RequestAttribute("userId") UUID userId) {
        String role = getUserRole(userId);
        UUID regionId = null;
        UUID districtId = null;

        if ("REGIONAL_ADMIN".equals(role)) {
            regionId = getUserRegionId(userId);
        } else if ("DISTRICT_ADMIN".equals(role)) {
            districtId = getUserDistrictId(userId);
            if (districtId != null) {
                regionId = getUserRegionId(userId);
            }
        }

        return ResponseEntity.ok(oversightService.getCurriculum(regionId, districtId));
    }

    @GetMapping("/assessments")
    @PreAuthorize("hasAnyRole('NATIONAL_ADMIN', 'REGIONAL_ADMIN', 'DISTRICT_ADMIN')")
    public ResponseEntity<AssessmentsResponse> getAssessments(
            @RequestAttribute("userId") UUID userId) {
        String role = getUserRole(userId);
        UUID regionId = null;
        UUID districtId = null;

        if ("REGIONAL_ADMIN".equals(role)) {
            regionId = getUserRegionId(userId);
        } else if ("DISTRICT_ADMIN".equals(role)) {
            districtId = getUserDistrictId(userId);
            if (districtId != null) {
                regionId = getUserRegionId(userId);
            }
        }

        return ResponseEntity.ok(oversightService.getAssessments(regionId, districtId));
    }

    @GetMapping("/live-classes")
    @PreAuthorize("hasAnyRole('NATIONAL_ADMIN', 'REGIONAL_ADMIN', 'DISTRICT_ADMIN')")
    public ResponseEntity<LiveClassesResponse> getLiveClasses(
            @RequestAttribute("userId") UUID userId) {
        String role = getUserRole(userId);
        UUID regionId = null;
        UUID districtId = null;

        if ("REGIONAL_ADMIN".equals(role)) {
            regionId = getUserRegionId(userId);
        } else if ("DISTRICT_ADMIN".equals(role)) {
            districtId = getUserDistrictId(userId);
            if (districtId != null) {
                regionId = getUserRegionId(userId);
            }
        }

        return ResponseEntity.ok(oversightService.getLiveClasses(regionId, districtId));
    }

    @GetMapping("/reports")
    @PreAuthorize("hasAnyRole('NATIONAL_ADMIN', 'REGIONAL_ADMIN', 'DISTRICT_ADMIN')")
    public ResponseEntity<List<ReportSummary>> getReports() {
        return ResponseEntity.ok(oversightService.getReports());
    }

    @GetMapping("/alerts")
    @PreAuthorize("hasAnyRole('NATIONAL_ADMIN', 'REGIONAL_ADMIN', 'DISTRICT_ADMIN')")
    public ResponseEntity<AlertsResponse> getAlerts(
            @RequestAttribute("userId") UUID userId) {
        String role = getUserRole(userId);
        UUID regionId = null;
        UUID districtId = null;

        if ("REGIONAL_ADMIN".equals(role)) {
            regionId = getUserRegionId(userId);
        } else if ("DISTRICT_ADMIN".equals(role)) {
            districtId = getUserDistrictId(userId);
            if (districtId != null) {
                regionId = getUserRegionId(userId);
            }
        }

        return ResponseEntity.ok(oversightService.getAlerts(regionId, districtId));
    }

    @GetMapping("/live-classes/{liveClassId}/observe")
    @PreAuthorize("hasAnyRole('NATIONAL_ADMIN', 'REGIONAL_ADMIN', 'DISTRICT_ADMIN')")
    public ResponseEntity<ObserverJoinResponse> getObserverJoinUrl(
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID liveClassId) {
        return ResponseEntity.ok(oversightService.getObserverJoinUrl(userId, liveClassId));
    }
}