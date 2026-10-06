package tz.elmkusoma.oversight.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.administration.dto.GlobalSearchResult;
import tz.elmkusoma.oversight.dto.*;
import tz.elmkusoma.oversight.service.OversightGovernanceService;
import tz.elmkusoma.oversight.service.OversightScopeResolver;
import tz.elmkusoma.oversight.service.OversightScopeResolver.Scope;
import tz.elmkusoma.oversight.service.OversightService;
import tz.elmkusoma.shared.domain.Institution;
import tz.elmkusoma.shared.repository.InstitutionRepository;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/oversight")
@RequiredArgsConstructor
public class OversightController {

    private final OversightService oversightService;
    private final OversightGovernanceService governanceService;
    private final OversightScopeResolver scopeResolver;
    private final InstitutionRepository institutionRepository;

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('NATIONAL_ADMIN', 'REGIONAL_ADMIN', 'DISTRICT_ADMIN')")
    public ResponseEntity<OversightDashboardResponse> getDashboard(
            @RequestAttribute("userId") UUID userId,
            @RequestParam(required = false) UUID regionId,
            @RequestParam(required = false) UUID districtId) {
        Scope scope = scopeResolver.resolve(userId, regionId, districtId);
        return ResponseEntity.ok(oversightService.getDashboard(scope.regionId(), scope.districtId()));
    }

    @GetMapping("/regions")
    @PreAuthorize("hasAnyRole('ADMIN', 'NATIONAL_ADMIN', 'REGIONAL_ADMIN')")
    public ResponseEntity<List<RegionResponse>> getRegions(
            @RequestAttribute("userId") UUID userId) {
        Scope scope = scopeResolver.resolve(userId, null, null);
        List<RegionResponse> regions = oversightService.getAllRegions();
        if (scope.regionId() != null) {
            UUID ownRegionId = scope.regionId();
            regions = regions.stream().filter(r -> ownRegionId.equals(r.getId())).toList();
        }
        return ResponseEntity.ok(regions);
    }

    @GetMapping("/regions/{regionId}/districts")
    @PreAuthorize("hasAnyRole('ADMIN', 'NATIONAL_ADMIN', 'REGIONAL_ADMIN', 'DISTRICT_ADMIN')")
    public ResponseEntity<List<DistrictResponse>> getDistricts(
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID regionId) {
        Scope scope = scopeResolver.resolve(userId, regionId, null);
        List<DistrictResponse> districts = oversightService.getDistrictsByRegion(scope.regionId());
        if (scope.districtId() != null) {
            // District admins only ever see their own district, never siblings.
            UUID ownDistrictId = scope.districtId();
            districts = districts.stream().filter(d -> ownDistrictId.equals(d.getId())).toList();
        }
        return ResponseEntity.ok(districts);
    }

    @GetMapping("/regions/{regionId}/institutions")
    @PreAuthorize("hasAnyRole('ADMIN', 'NATIONAL_ADMIN', 'REGIONAL_ADMIN')")
    public ResponseEntity<List<InstitutionSummary>> getInstitutionsByRegion(
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID regionId) {
        Scope scope = scopeResolver.resolve(userId, regionId, null);
        return ResponseEntity.ok(oversightService.getInstitutionsByRegion(scope.regionId()));
    }

    @GetMapping("/districts/{districtId}/institutions")
    @PreAuthorize("hasAnyRole('ADMIN', 'NATIONAL_ADMIN', 'REGIONAL_ADMIN', 'DISTRICT_ADMIN')")
    public ResponseEntity<List<InstitutionSummary>> getInstitutionsByDistrict(
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID districtId) {
        Scope scope = scopeResolver.resolve(userId, null, districtId);
        return ResponseEntity.ok(oversightService.getInstitutionsByDistrict(scope.districtId()));
    }

    @GetMapping("/institutions/{institutionId}")
    @PreAuthorize("hasAnyRole('NATIONAL_ADMIN', 'REGIONAL_ADMIN', 'DISTRICT_ADMIN')")
    public ResponseEntity<InstitutionDetailResponse> getInstitutionDetail(
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID institutionId) {
        Institution institution = institutionRepository.findById(institutionId)
                .orElseThrow(() -> new tz.elmkusoma.exception.ResourceNotFoundException(
                        "Institution", "id", institutionId));
        verifyInstitutionInScope(userId, institution);
        return ResponseEntity.ok(oversightService.getInstitutionDetail(institutionId));
    }

    private void verifyInstitutionInScope(UUID userId, Institution institution) {
        // Reuses the same rule as every other endpoint: the institution must
        // fall inside the caller's resolved jurisdiction.
        Scope scope = scopeResolver.resolve(userId, null, null);
        if (scope.districtId() != null) {
            if (!scope.districtId().equals(institution.getDistrictId())) {
                throw new tz.elmkusoma.exception.ForbiddenException("institution", "access");
            }
        } else if (scope.regionId() != null) {
            if (!scope.regionId().equals(institution.getRegionId())) {
                throw new tz.elmkusoma.exception.ForbiddenException("institution", "access");
            }
        }
    }

    @GetMapping("/schools")
    @PreAuthorize("hasAnyRole('NATIONAL_ADMIN', 'REGIONAL_ADMIN', 'DISTRICT_ADMIN')")
    public ResponseEntity<List<InstitutionSummary>> getSchools(
            @RequestAttribute("userId") UUID userId,
            @RequestParam(required = false) UUID regionId,
            @RequestParam(required = false) UUID districtId) {
        Scope scope = scopeResolver.resolve(userId, regionId, districtId);

        List<InstitutionSummary> schools;
        if (scope.districtId() != null) {
            schools = oversightService.getInstitutionsByDistrict(scope.districtId());
        } else {
            schools = oversightService.getInstitutionsByRegion(scope.regionId());
        }
        return ResponseEntity.ok(schools);
    }

    @GetMapping("/performance")
    @PreAuthorize("hasAnyRole('NATIONAL_ADMIN', 'REGIONAL_ADMIN', 'DISTRICT_ADMIN')")
    public ResponseEntity<PerformanceResponse> getPerformance(
            @RequestAttribute("userId") UUID userId,
            @RequestParam(required = false) UUID regionId,
            @RequestParam(required = false) UUID districtId) {
        Scope scope = scopeResolver.resolve(userId, regionId, districtId);
        return ResponseEntity.ok(oversightService.getPerformance(scope.regionId(), scope.districtId()));
    }

    @GetMapping("/attendance")
    @PreAuthorize("hasAnyRole('NATIONAL_ADMIN', 'REGIONAL_ADMIN', 'DISTRICT_ADMIN')")
    public ResponseEntity<AttendanceResponse> getAttendance(
            @RequestAttribute("userId") UUID userId,
            @RequestParam(required = false) UUID regionId,
            @RequestParam(required = false) UUID districtId) {
        Scope scope = scopeResolver.resolve(userId, regionId, districtId);
        return ResponseEntity.ok(oversightService.getAttendance(scope.regionId(), scope.districtId()));
    }

    @GetMapping("/curriculum")
    @PreAuthorize("hasAnyRole('NATIONAL_ADMIN', 'REGIONAL_ADMIN', 'DISTRICT_ADMIN')")
    public ResponseEntity<CurriculumResponse> getCurriculum(
            @RequestAttribute("userId") UUID userId,
            @RequestParam(required = false) UUID regionId,
            @RequestParam(required = false) UUID districtId) {
        Scope scope = scopeResolver.resolve(userId, regionId, districtId);
        return ResponseEntity.ok(oversightService.getCurriculum(scope.regionId(), scope.districtId()));
    }

    @GetMapping("/assessments")
    @PreAuthorize("hasAnyRole('NATIONAL_ADMIN', 'REGIONAL_ADMIN', 'DISTRICT_ADMIN')")
    public ResponseEntity<AssessmentsResponse> getAssessments(
            @RequestAttribute("userId") UUID userId,
            @RequestParam(required = false) UUID regionId,
            @RequestParam(required = false) UUID districtId) {
        Scope scope = scopeResolver.resolve(userId, regionId, districtId);
        return ResponseEntity.ok(oversightService.getAssessments(scope.regionId(), scope.districtId()));
    }

    @GetMapping("/live-classes")
    @PreAuthorize("hasAnyRole('NATIONAL_ADMIN', 'REGIONAL_ADMIN', 'DISTRICT_ADMIN')")
    public ResponseEntity<LiveClassesResponse> getLiveClasses(
            @RequestAttribute("userId") UUID userId,
            @RequestParam(required = false) UUID regionId,
            @RequestParam(required = false) UUID districtId) {
        Scope scope = scopeResolver.resolve(userId, regionId, districtId);
        return ResponseEntity.ok(oversightService.getLiveClasses(scope.regionId(), scope.districtId()));
    }

    @GetMapping("/reports")
    @PreAuthorize("hasAnyRole('NATIONAL_ADMIN', 'REGIONAL_ADMIN', 'DISTRICT_ADMIN')")
    public ResponseEntity<List<ReportSummary>> getReports() {
        return ResponseEntity.ok(oversightService.getReports());
    }

    @GetMapping("/alerts")
    @PreAuthorize("hasAnyRole('NATIONAL_ADMIN', 'REGIONAL_ADMIN', 'DISTRICT_ADMIN')")
    public ResponseEntity<AlertsResponse> getAlerts(
            @RequestAttribute("userId") UUID userId,
            @RequestParam(required = false) UUID regionId,
            @RequestParam(required = false) UUID districtId) {
        Scope scope = scopeResolver.resolve(userId, regionId, districtId);
        return ResponseEntity.ok(oversightService.getAlerts(scope.regionId(), scope.districtId()));
    }

    @GetMapping("/attention")
    @PreAuthorize("hasAnyRole('NATIONAL_ADMIN', 'REGIONAL_ADMIN', 'DISTRICT_ADMIN')")
    public ResponseEntity<AttentionResponse> getAttention(
            @RequestAttribute("userId") UUID userId,
            @RequestParam(required = false) UUID regionId,
            @RequestParam(required = false) UUID districtId) {
        Scope scope = scopeResolver.resolve(userId, regionId, districtId);
        return ResponseEntity.ok(governanceService.getAttention(scope.regionId(), scope.districtId()));
    }

    @GetMapping("/data-quality")
    @PreAuthorize("hasAnyRole('NATIONAL_ADMIN', 'REGIONAL_ADMIN', 'DISTRICT_ADMIN')")
    public ResponseEntity<DataQualityResponse> getDataQuality(
            @RequestAttribute("userId") UUID userId,
            @RequestParam(required = false) UUID regionId,
            @RequestParam(required = false) UUID districtId) {
        Scope scope = scopeResolver.resolve(userId, regionId, districtId);
        return ResponseEntity.ok(governanceService.getDataQuality(scope.regionId(), scope.districtId()));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('NATIONAL_ADMIN', 'REGIONAL_ADMIN', 'DISTRICT_ADMIN')")
    public ResponseEntity<List<GlobalSearchResult>> search(
            @RequestAttribute("userId") UUID userId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) UUID regionId,
            @RequestParam(required = false) UUID districtId) {
        Scope scope = scopeResolver.resolve(userId, regionId, districtId);
        return ResponseEntity.ok(governanceService.search(q, type, scope.regionId(), scope.districtId()));
    }

    @GetMapping("/reports/{reportId}/export")
    @PreAuthorize("hasAnyRole('NATIONAL_ADMIN', 'REGIONAL_ADMIN', 'DISTRICT_ADMIN')")
    public ResponseEntity<byte[]> exportReport(
            @RequestAttribute("userId") UUID userId,
            @PathVariable String reportId,
            @RequestParam(required = false) UUID regionId,
            @RequestParam(required = false) UUID districtId) {
        Scope scope = scopeResolver.resolve(userId, regionId, districtId);
        String csv = governanceService.exportReport(reportId, scope.regionId(), scope.districtId());
        String filename = reportId + "_" + LocalDate.now() + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "text/csv")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/live-classes/{liveClassId}/observe")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'NATIONAL_ADMIN', 'REGIONAL_ADMIN', 'DISTRICT_ADMIN')")
    public ResponseEntity<ObserverJoinResponse> getObserverJoinUrl(
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID liveClassId) {
        return ResponseEntity.ok(oversightService.getObserverJoinUrl(userId, liveClassId));
    }
}
