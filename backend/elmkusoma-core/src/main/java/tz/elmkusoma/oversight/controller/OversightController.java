package tz.elmkusoma.oversight.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.oversight.dto.*;
import tz.elmkusoma.oversight.service.OversightService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/oversight")
@RequiredArgsConstructor
public class OversightController {

    private final OversightService oversightService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('NATIONAL_ADMIN', 'REGIONAL_ADMIN', 'DISTRICT_ADMIN')")
    public ResponseEntity<OversightDashboardResponse> getDashboard(
            @RequestParam(required = false) UUID regionId,
            @RequestParam(required = false) UUID districtId) {
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
}
