package tz.elmkusoma.oversight.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tz.elmkusoma.exception.ForbiddenException;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.oversight.domain.District;
import tz.elmkusoma.oversight.repository.DistrictRepository;
import tz.elmkusoma.oversight.repository.RegionRepository;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.UserRepository;

import java.util.UUID;

/**
 * The single authority for oversight jurisdiction (Nationaladmin.md §5/§8).
 *
 * <p>Client-supplied {@code regionId}/{@code districtId} are only ever
 * <em>narrowed into</em> (or validated inside) the caller's own authority —
 * never widened. National admins may drill down anywhere, regional admins only
 * inside their own region, district admins only their own district. Every
 * oversight controller resolves scope through this component so no endpoint
 * can re-implement (and weaken) the rule.</p>
 */
@Component
@RequiredArgsConstructor
public class OversightScopeResolver {

    private final UserRepository userRepository;
    private final RegionRepository regionRepository;
    private final DistrictRepository districtRepository;

    public record Scope(UUID regionId, UUID districtId) {}

    public Scope resolve(UUID userId, UUID requestedRegionId, UUID requestedDistrictId) {
        String role = getUserRole(userId);
        if (role == null) {
            throw new ForbiddenException("oversight", "access");
        }

        if ("NATIONAL_ADMIN".equals(role) || "ADMIN".equals(role)) {
            if (requestedDistrictId != null) {
                District district = districtRepository.findById(requestedDistrictId)
                        .orElseThrow(() -> new ResourceNotFoundException("District", "id", requestedDistrictId));
                if (requestedRegionId != null && !requestedRegionId.equals(district.getRegionId())) {
                    throw new ForbiddenException("district", "select outside its region");
                }
                return new Scope(district.getRegionId(), district.getId());
            }
            if (requestedRegionId != null) {
                if (!regionRepository.existsById(requestedRegionId)) {
                    throw new ResourceNotFoundException("Region", "id", requestedRegionId);
                }
                return new Scope(requestedRegionId, null);
            }
            return new Scope(null, null);
        }

        if ("REGIONAL_ADMIN".equals(role)) {
            UUID ownRegionId = getUserRegionId(userId);
            if (ownRegionId == null) {
                throw new ForbiddenException("region", "manage");
            }
            if (requestedRegionId != null && !ownRegionId.equals(requestedRegionId)) {
                throw new ForbiddenException("region", "access");
            }
            if (requestedDistrictId != null) {
                District district = districtRepository.findById(requestedDistrictId)
                        .orElseThrow(() -> new ResourceNotFoundException("District", "id", requestedDistrictId));
                if (!ownRegionId.equals(district.getRegionId())) {
                    throw new ForbiddenException("district", "access");
                }
                return new Scope(ownRegionId, district.getId());
            }
            return new Scope(ownRegionId, null);
        }

        if ("DISTRICT_ADMIN".equals(role)) {
            UUID ownDistrictId = getUserDistrictId(userId);
            UUID ownRegionId = getUserRegionId(userId);
            if (ownDistrictId == null) {
                throw new ForbiddenException("district", "manage");
            }
            if (requestedDistrictId != null && !ownDistrictId.equals(requestedDistrictId)) {
                throw new ForbiddenException("district", "access");
            }
            if (requestedRegionId != null && ownRegionId != null && !ownRegionId.equals(requestedRegionId)) {
                throw new ForbiddenException("region", "access");
            }
            return new Scope(ownRegionId, ownDistrictId);
        }

        throw new ForbiddenException("oversight", "access");
    }

    private String getUserRole(UUID userId) {
        return userRepository.findById(userId)
                .map(u -> u.getRole().name())
                .orElse(null);
    }

    private UUID getUserRegionId(UUID userId) {
        return userRepository.findById(userId).map(User::getRegionId).orElse(null);
    }

    private UUID getUserDistrictId(UUID userId) {
        return userRepository.findById(userId).map(User::getDistrictId).orElse(null);
    }
}
