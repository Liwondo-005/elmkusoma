package tz.elmkusoma.liveclass.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.liveclass.domain.MediaAsset;
import tz.elmkusoma.liveclass.dto.MediaAssetResponse;
import tz.elmkusoma.liveclass.dto.MediaAssetRequest;
import tz.elmkusoma.liveclass.repository.MediaAssetRepository;
import tz.elmkusoma.liveclass.service.MediaProxyService;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/media")
@RequiredArgsConstructor
@Tag(name = "Media Library", description = "Media assets for live classes and recordings")
public class MediaLibraryController {

    private final MediaAssetRepository mediaAssetRepository;
    private final MediaProxyService mediaProxyService;
    private final tz.elmkusoma.shared.repository.InstitutionMembershipRepository membershipRepository;
    private final tz.elmkusoma.liveclass.repository.LiveClassParticipantRepository participantRepository;
    private final tz.elmkusoma.course.repository.LiveClassRepository liveClassRepository;
    private final tz.elmkusoma.liveclass.service.RecordingPlaybackUrls recordingPlaybackUrls;
    private final tz.elmkusoma.liveclass.service.LiveClassRecordingAccessService liveClassRecordingAccessService;
    private final tz.elmkusoma.common.AudienceTargetGuard audienceTargetGuard;

    /** Server-written origin that marks an asset as a live-class recording. */
    private static final String SOURCE_LIVE_CLASS = "LIVE_CLASS";

    /**
     * Application-owned playback path for a LIVE_CLASS asset, or {@code null} for any other
     * asset.
     *
     * <p>The class id is read from the persisted {@code source_id} written by the recording
     * hand-off - never from client input - so a caller cannot point playback at another class's
     * object key. The resulting path is the ordinary authorised
     * {@code /v1/live-session/classes/{id}/recording/playback} endpoint, which re-checks
     * authentication, institution, ownership and participation on every request (including each
     * Range request). Fail-closed: if the frontend URL is unavailable, return {@code null} rather
     * than falling back to the storage location.</p>
     */
    private String liveClassPlaybackUrl(MediaAsset asset) {
        if (asset == null || asset.getSourceId() == null) {
            return null;
        }
        return SOURCE_LIVE_CLASS.equalsIgnoreCase(asset.getSourceType())
                ? recordingPlaybackUrls.forLiveClass(asset.getSourceId())
                : null;
    }

    /**
     * Records whose class-level target must be enforced rather than merely filtered in the UI.
     *
     * <p>Recording assets are learner-facing recordings of a specific class. Institution
     * membership alone is not sufficient: before this check any student in the institution
     * could list and download every other class's recordings.</p>
     *
     * <p>A learner passes on either of two server-derived conditions, both of which also gate
     * the Replay Library, so the two libraries can never disagree about the same recording:</p>
     * <ul>
     *   <li><b>Attendance</b> - a {@code LiveClassParticipant} row for the recording's live
     *       class. This was the whole of the old rule and is kept first and unchanged.</li>
     *   <li><b>Audience membership</b> - membership of the class group the recording was made
     *       for, resolved through the same union {@code LiveSessionController} already required
     *       before letting the learner join that class. Attendance was being used as a proxy for
     *       entitlement, which denied learners who were entitled to the class but absent on the
     *       day, or who enrolled after it was recorded.</li>
     * </ul>
     *
     * <p>Two server-derived conditions decide access without any tenant context at all:</p>
     * <ul>
     *   <li><b>Open learning</b> - a PUBLISHED + PUBLIC session declaring no private class
     *       scope has a platform-wide audience, and a playable recording of it follows that
     *       audience across institutions and to institution-less learners. This mirrors
     *       {@code LiveClassRecordingAccessService} for per-class replay.</li>
     *   <li><b>Ownership</b> - the teacher who made the material, which needs no tenant
     *       claim, so an independent (school-less) teacher manages their own uploads and
     *       recordings like any other teacher.</li>
     * </ul>
     *
     * <p>Staff keep their existing access inside their own institution: platform admins
     * (ADMIN / NATIONAL_ADMIN) remain a deliberate cross-tenant oversight exception, an
     * institution admin sees the whole library of their own institution — and no longer
     * skips the tenant boundary for another one — and a teacher sees their own live
     * classes.</p>
     */
    private boolean canViewAsset(MediaAsset asset, UUID callerUserId, UUID institutionId, String userRole) {
        // Platform oversight is not bounded by a tenant (unchanged, checked first).
        if (isPlatformOversight(userRole)) {
            return true;
        }
        // Institution-scoped staff keep whole-library access INSIDE their own tenant —
        // including a failed capture they are diagnosing — but the tenant boundary below
        // now applies to them as well. It used to be checked after a blanket bypass, so an
        // institution admin could fetch another tenant's asset (metadata and download URL)
        // by id; listing was always tenant-scoped, so no legitimate in-tenant flow changes.
        if ("INSTITUTION_ADMIN".equals(userRole)
                && institutionId != null && institutionId.equals(asset.getInstitutionId())) {
            return true;
        }
        // Ownership precedes tenancy: a teacher's own uploads and recordings need no tenant
        // claim — which is how a school-less (independent) teacher reaches their own material.
        // The caller id is server-derived from the authenticated principal, so this crosses
        // no tenant: it can only ever match rows this same user created.
        if (callerUserId != null && callerUserId.equals(asset.getTeacherId())) {
            return true;
        }
        // Open learning: a PUBLISHED + PUBLIC session that declares no private class scope
        // has, by definition, a platform-wide audience, and a playable recording of it
        // follows that audience — to every school and to institution-less learners alike.
        // This is the same decision LiveClassRecordingAccessService already enforces for
        // per-class replay, so listing, detail, download and playback cannot disagree.
        // Unpublished, private, class-scoped, failed or deleted material never gets here.
        if ("RECORDING".equalsIgnoreCase(asset.getMediaType())
                && callerUserId != null
                && isPlayableForLearners(asset)
                && SOURCE_LIVE_CLASS.equalsIgnoreCase(asset.getSourceType())
                && asset.getSourceId() != null
                && isOpenLearningSource(asset)) {
            return true;
        }
        // Institution boundary: every other asset never crosses tenants.
        if (institutionId == null || !institutionId.equals(asset.getInstitutionId())) {
            return false;
        }
        if (!"RECORDING".equalsIgnoreCase(asset.getMediaType())) {
            return true;
        }
        if (callerUserId == null) {
            return false;
        }
        // A capture that never produced a playable file is not something to offer a learner.
        // Staff still reach it through the branches above (institution-scoped diagnosis, or
        // the teacher who owns it), which is how a failed job is diagnosed.
        if (!isPlayableForLearners(asset)) {
            return false;
        }
        // Recordings originate from a live class; entitlement follows that class's audience.
        if ("LIVE_CLASS".equalsIgnoreCase(asset.getSourceType()) && asset.getSourceId() != null) {
            // 1. The learner was in the recorded session.
            if (participantRepository
                    .existsByLiveClassIdAndUserIdAndIsDeletedFalse(asset.getSourceId(), callerUserId)) {
                return true;
            }
            // 2. The learner belongs to the class the recording was made for.
            return isInRecordingAudience(asset, callerUserId, institutionId);
        }
        // A recording with no live-class origin has no participation evidence: deny rather
        // than expose institution-wide, matching Replay's default-deny rule.
        return false;
    }

    /**
     * Whether a learner may be shown this recording at all, judged apart from entitlement.
     *
     * <p>Entitlement answers "may you watch this class's recording"; this answers "is there a
     * recording to watch". The publishing path writes {@code READY} on success and {@code FAILED}
     * when the capture job could not produce a file, so a failed capture stays visible to the
     * teacher who owns it (checked earlier, before this runs) but is never advertised to the
     * learners it was meant for. An absent status is treated as playable because the column
     * predates the publisher and rows written before it existed still have to play.</p>
     */
    private static boolean isPlayableForLearners(MediaAsset asset) {
        String status = asset.getStatus();
        return status == null || status.isBlank()
                || "READY".equalsIgnoreCase(status)
                || "AVAILABLE".equalsIgnoreCase(status);
    }

    /**
     * Membership of the class group this recording was made for.
     *
     * <p>The class id is read from {@code media_assets.class_group_id}, which the recording
     * publisher copies from the live class it published for - never from the request - and a
     * live class is consulted only for rows written before that inheritance existed. Both
     * answers are server-written, so a caller cannot aim the check at a class of their choosing,
     * and a recording that names no class at all grants nothing: default-deny, exactly as
     * before.</p>
     */
    private boolean isInRecordingAudience(MediaAsset asset, UUID callerUserId, UUID institutionId) {
        if (asset.getClassGroupId() != null) {
            return liveClassRecordingAccessService.isLearnerInClassAudience(
                    callerUserId, institutionId, asset.getClassGroupId());
        }
        // Legacy row with no inherited class group: fall back to the live class it came from.
        return liveClassRepository.findById(asset.getSourceId())
                .filter(lc -> !Boolean.TRUE.equals(lc.getIsDeleted()))
                .map(tz.elmkusoma.course.domain.LiveClass::getClassGroupId)
                .map(classGroupId -> liveClassRecordingAccessService.isLearnerInClassAudience(
                        callerUserId, institutionId, classGroupId))
                .orElse(false);
    }

    private static boolean isPlatformOversight(String userRole) {
        return "ADMIN".equals(userRole)
                || "NATIONAL_ADMIN".equals(userRole);
    }

    /**
     * Whether a recording's source session is an open learning session: PUBLISHED + PUBLIC +
     * no declared class scope, on a row that still exists. The answer is read from the
     * persisted session — never from client input — and a missing or deleted source grants
     * nothing (default-deny), which is what keeps the branch below closed for legacy rows
     * whose recorded session never existed or has been removed.
     */
    private boolean isOpenLearningSource(MediaAsset asset) {
        return liveClassRepository.findById(asset.getSourceId())
                .filter(lc -> !Boolean.TRUE.equals(lc.getIsDeleted()))
                .map(tz.elmkusoma.course.domain.LiveClass::isOpenLearningSession)
                .orElse(false);
    }

    /**
     * Origin keys that only server-side code may write.
     *
     * <p>Case-insensitive to match {@link #canViewAsset}, which resolves replay entitlement with
     * {@code "LIVE_CLASS".equalsIgnoreCase(asset.getSourceType())} - a caller must not be able to
     * slip past the guard with {@code live_class}.
     */
    private static boolean isReservedOrigin(String sourceType) {
        return sourceType != null && "LIVE_CLASS".equalsIgnoreCase(sourceType.trim());
    }

    /** Guards the class/recording listing used by the teacher media pages. */
    private boolean isMemberOfInstitution(UUID callerUserId, UUID institutionId) {
        return callerUserId != null && institutionId != null
                && membershipRepository.existsByUserIdAndInstitutionIdAndIsActiveTrue(callerUserId, institutionId);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','OTHER_LEARNER','INSTITUTION_ADMIN')")
    @Operation(summary = "List media assets for institution")
    public ResponseEntity<ApiResponse<List<MediaAssetResponse>>> listMedia(
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute(value = "institutionId", required = false) UUID institutionId,
            @RequestAttribute(value = "userRole", required = false) String userRole,
            @RequestParam(required = false) String type) {

        // The tenant must come from the server-derived attribute, and the caller must
        // actually belong to it. Without this check any authenticated user could set
        // X-Institution-Id to another tenant and read that tenant's media library. An
        // account that claims no tenant at all (an independent teacher or learner) skips
        // this guard instead of failing it: what such a reader may list below is decided
        // per asset by canViewAsset, never by a claim they could make up.
        if (institutionId != null && !isMemberOfInstitution(userId, institutionId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("You are not a member of this institution"));
        }

        // The caller's own tenant library. A caller with no tenant contributes no rows
        // here, and the query is scoped rather than post-filtered, so no amount of
        // entitlement below could ever widen this set beyond the caller's institution.
        java.util.LinkedHashMap<UUID, MediaAsset> assets = new java.util.LinkedHashMap<>();
        if (institutionId != null) {
            if (type != null && !type.isEmpty()) {
                mediaAssetRepository
                        .findByInstitutionIdAndMediaTypeAndIsDeletedFalseOrderByCreatedAtDesc(institutionId, type)
                        .forEach(a -> assets.put(a.getId(), a));
            } else {
                mediaAssetRepository.findByInstitutionIdAndIsDeletedFalseOrderByCreatedAtDesc(institutionId)
                        .forEach(a -> assets.put(a.getId(), a));
            }
        } else {
            // No tenant to scope by: the reader's own material — authorization by
            // ownership, which crosses no tenant — joins the open-learning union below.
            // For a learner this query is always empty (nothing is ever owned by them);
            // it is what lets an independent teacher's own uploads appear in a listing
            // that has no institution to be scoped by. The type filter still applies.
            mediaAssetRepository.findByTeacherIdAndIsDeletedFalseOrderByCreatedAtDesc(userId)
                    .stream()
                    .filter(a -> type == null || type.isEmpty() || type.equalsIgnoreCase(a.getMediaType()))
                    .forEach(a -> assets.put(a.getId(), a));
        }

        // Open-learning discovery: recordings of PUBLISHED + PUBLIC sessions that declare
        // no private class scope are listed for every eligible reader — across institutions
        // and for institution-less learners alike. This is the only union allowed to cross
        // the tenant boundary, its candidate set is server-derived (findOpenLearningSessions
        // plus this library's own RECORDING filter — never request input), and every row is
        // still entitlement-filtered below, so a failed, unpublished or deleted capture of an
        // open session is never advertised either.
        if (type == null || type.isEmpty() || "RECORDING".equalsIgnoreCase(type)) {
            openLearningRecordings().forEach(a -> assets.putIfAbsent(a.getId(), a));
        }

        // Recordings are filtered by entitlement server-side. Filtering in the UI is not
        // authorization, and without this every student would see every class's recording.
        List<MediaAssetResponse> response = assets.values().stream()
                .filter(a -> canViewAsset(a, userId, institutionId, userRole))
                .map(this::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Recordings of every currently open learning session (PUBLISHED + PUBLIC + no declared
     * class scope), newest first. The candidate set comes from
     * {@code findOpenLearningSessions} — the same server-side decision that governs learner
     * discovery and join — so no request input can widen it. Per-asset entitlement is still
     * re-checked by {@link #canViewAsset}, which is what keeps failed or unpublished rows of
     * an open session out of every listing.
     */
    private List<MediaAsset> openLearningRecordings() {
        List<tz.elmkusoma.course.domain.LiveClass> openSessions =
                liveClassRepository.findOpenLearningSessions();
        if (openSessions.isEmpty()) {
            return List.of();
        }
        java.util.Set<UUID> openIds = openSessions.stream()
                .map(tz.elmkusoma.course.domain.LiveClass::getId)
                .collect(Collectors.toSet());
        return mediaAssetRepository
                .findBySourceTypeAndSourceIdInAndMediaTypeAndIsDeletedFalseOrderByCreatedAtDesc(
                        SOURCE_LIVE_CLASS, openIds, "RECORDING");
    }

    @GetMapping("/recordings/{sourceType}/{sourceId}")
    @PreAuthorize("hasAnyRole('TEACHER','STUDENT','OTHER_LEARNER','INSTITUTION_ADMIN')")
    @Operation(summary = "Get recordings for a live class or event")
    public ResponseEntity<ApiResponse<List<MediaAssetResponse>>> getRecordings(
            @PathVariable String sourceType,
            @PathVariable UUID sourceId,
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute(value = "institutionId", required = false) UUID institutionId,
            @RequestAttribute(value = "userRole", required = false) String userRole) {

        // A tenant claim, when made, must be proven — but an account with no tenant at all
        // (independent teacher or learner) proceeds: canViewAsset below decides per asset,
        // and an open session's recordings are exactly the case that must be reachable
        // without a school while every private class's remain closed to them.
        if (institutionId != null && !isMemberOfInstitution(userId, institutionId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("You are not a member of this institution"));
        }

        // Entitlement is per asset, not just per tenant: a learner may open the recordings of
        // sessions they took part in and must not enumerate another class's recordings by id.
        List<MediaAssetResponse> response = mediaAssetRepository
                .findBySourceTypeAndSourceIdAndIsDeletedFalse(sourceType, sourceId)
                .stream()
                .filter(a -> canViewAsset(a, userId, institutionId, userRole))
                .map(this::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/class/{classGroupId}")
    @PreAuthorize("hasAnyRole('TEACHER','STUDENT','OTHER_LEARNER','INSTITUTION_ADMIN')")
    @Operation(summary = "Get recording assets targeted at a class group")
    public ResponseEntity<ApiResponse<List<MediaAssetResponse>>> getRecordingsByClass(
            @PathVariable UUID classGroupId,
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute(value = "institutionId", required = false) UUID institutionId,
            @RequestAttribute(value = "userRole", required = false) String userRole) {

        // Class targeting is inherently institution-scoped (a class group belongs to one
        // school), so a tenant claim must be proven; an account without a tenant simply has
        // no class-scoped rows to select here — the selector below returns none for them,
        // while open sessions remain reachable through the source and listing endpoints.
        if (institutionId != null && !isMemberOfInstitution(userId, institutionId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("You are not a member of this institution"));
        }

        // The class id is only a selector; the asset's own institution and entitlement are
        // still enforced, so a guessed class id cannot widen access.
        List<MediaAssetResponse> response = mediaAssetRepository
                .findByInstitutionIdAndClassGroupIdAndMediaTypeAndIsDeletedFalse(
                        institutionId, classGroupId, "RECORDING")
                .stream()
                .filter(a -> canViewAsset(a, userId, institutionId, userRole))
                .map(this::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER','STUDENT','OTHER_LEARNER','INSTITUTION_ADMIN')")
    @Operation(summary = "Get media asset detail")
    public ResponseEntity<ApiResponse<MediaAssetResponse>> getMedia(
            @PathVariable UUID id,
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute(value = "institutionId", required = false) UUID institutionId,
            @RequestAttribute(value = "userRole", required = false) String userRole) {
        MediaAsset asset = mediaAssetRepository.findById(id)
                .filter(a -> !Boolean.TRUE.equals(a.getIsDeleted()))
                .orElse(null);
        if (asset == null || !canViewAsset(asset, userId, institutionId, userRole)) {
            // Same response as "missing" so this endpoint cannot be used to probe which
            // recordings exist in another class or another institution.
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Media asset not found"));
        }
        return ResponseEntity.ok(ApiResponse.success(toResponse(asset)));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Get teacher's media assets")
    public ResponseEntity<ApiResponse<List<MediaAssetResponse>>> myMedia(
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute(value = "institutionId", required = false) UUID institutionId) {

        if (institutionId == null) {
            // An independent (school-less) teacher still manages their own material: the
            // authorization here is ownership — rows keyed to this authenticated user —
            // and there is no tenant to scope by. Nothing else becomes reachable: this
            // query can only ever return rows whose teacher_id is the caller themselves.
            List<MediaAssetResponse> own = mediaAssetRepository
                    .findByTeacherIdAndIsDeletedFalseOrderByCreatedAtDesc(userId)
                    .stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(ApiResponse.success(own));
        }
        List<MediaAsset> assets = mediaAssetRepository
                .findByTeacherIdAndInstitutionIdAndIsDeletedFalseOrderByCreatedAtDesc(
                        userId, institutionId);

        List<MediaAssetResponse> response = assets.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('TEACHER','STUDENT','OTHER_LEARNER','INSTITUTION_ADMIN')")
    @Operation(summary = "Search media assets")
    public ResponseEntity<ApiResponse<List<MediaAssetResponse>>> searchMedia(
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute(value = "institutionId", required = false) UUID institutionId,
            @RequestAttribute(value = "userRole", required = false) String userRole,
            @RequestParam String q) {

        // Search covers the caller's own tenant library: a tenant claim must be proven, and
        // a reader without a tenant owns no searchable library here. It deliberately does
        // NOT substitute cross-tenant results — search must never become a side channel
        // around the entitlement check on listing; open-learning discovery lives there.
        if (institutionId != null && !isMemberOfInstitution(userId, institutionId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("You are not a member of this institution"));
        }

        // Search must not become a side channel around the entitlement check on listing.
        List<MediaAsset> assets = (institutionId == null ? List.<MediaAsset>of()
                        : mediaAssetRepository.searchByTitle(institutionId, q)).stream()
                .filter(a -> canViewAsset(a, userId, institutionId, userRole))
                .toList();

        List<MediaAssetResponse> response = assets.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER','INSTITUTION_ADMIN')")
    @Operation(summary = "Create a media asset")
    public ResponseEntity<ApiResponse<MediaAssetResponse>> createMedia(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute("userRole") String userRole,
            @RequestBody MediaAssetRequest request) {

        // LIVE_CLASS is a reserved origin: it is written only by the recording hand-off
        // (LiveRecordingMediaPublisher on the webhook path, publishRecordingToMediaLibrary on the
        // synchronous finalize path) and it is what canViewAsset uses to derive replay
        // entitlement. Letting a request claim it would let anyone attach an arbitrary file to
        // someone else's session and - with V150's unique (source_type, source_id) key - squat the
        // slot so the genuine recording can never be published.
        if (isReservedOrigin(request.getSourceType())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("sourceType LIVE_CLASS is set by the server, not the client"));
        }

        // classGroupId and subjectId decide who may later see this asset in their library, so they
        // are client claims that must be proven against the caller's institution. The role matters:
        // a teacher must also be assigned to the class, while an institution admin may target any
        // class in their own school.
        audienceTargetGuard.validateTarget(institutionId, request.getClassGroupId(),
                request.getSubjectId(), userId, userRole);

        MediaAsset asset = MediaAsset.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .mediaType(request.getMediaType())
                .fileUrl(request.getFileUrl())
                .thumbnailUrl(request.getThumbnailUrl())
                .durationSeconds(request.getDurationSeconds())
                .fileSizeBytes(request.getFileSizeBytes())
                .mimeType(request.getMimeType())
                .status(request.getStatus() != null ? request.getStatus() : "READY")
                .visibility(request.getVisibility() != null ? request.getVisibility() : "INSTITUTION")
                .sourceType(request.getSourceType())
                .sourceId(request.getSourceId())
                .teacherId(userId)
                .courseId(request.getCourseId())
                .classGroupId(request.getClassGroupId())
                .subjectId(request.getSubjectId())
                .tags(request.getTags())
                .build();
        asset.setInstitutionId(institutionId);
        asset = mediaAssetRepository.save(asset);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Media asset created", toResponse(asset)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER','INSTITUTION_ADMIN')")
    @Operation(summary = "Update a media asset")
    public ResponseEntity<ApiResponse<MediaAssetResponse>> updateMedia(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID id,
            @RequestBody MediaAssetRequest request) {

        MediaAsset asset = mediaAssetRepository.findById(id)
                .filter(a -> !Boolean.TRUE.equals(a.getIsDeleted()))
                .filter(a -> institutionId.equals(a.getInstitutionId()))
                .orElse(null);
        if (asset == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Media asset not found"));
        }

        if (request.getTitle() != null) asset.setTitle(request.getTitle());
        if (request.getDescription() != null) asset.setDescription(request.getDescription());
        if (request.getMediaType() != null) asset.setMediaType(request.getMediaType());
        if (request.getFileUrl() != null) asset.setFileUrl(request.getFileUrl());
        if (request.getThumbnailUrl() != null) asset.setThumbnailUrl(request.getThumbnailUrl());
        if (request.getDurationSeconds() != null) asset.setDurationSeconds(request.getDurationSeconds());
        if (request.getFileSizeBytes() != null) asset.setFileSizeBytes(request.getFileSizeBytes());
        if (request.getMimeType() != null) asset.setMimeType(request.getMimeType());
        if (request.getStatus() != null) asset.setStatus(request.getStatus());
        if (request.getVisibility() != null) asset.setVisibility(request.getVisibility());
        if (request.getTags() != null) asset.setTags(request.getTags());

        asset = mediaAssetRepository.save(asset);
        return ResponseEntity.ok(ApiResponse.success("Media asset updated", toResponse(asset)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER','INSTITUTION_ADMIN')")
    @Operation(summary = "Delete a media asset (soft delete)")
    public ResponseEntity<ApiResponse<Void>> deleteMedia(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable UUID id) {

        MediaAsset asset = mediaAssetRepository.findById(id)
                .filter(a -> !Boolean.TRUE.equals(a.getIsDeleted()))
                .filter(a -> institutionId.equals(a.getInstitutionId()))
                .orElse(null);
        if (asset == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Media asset not found"));
        }

        asset.setIsDeleted(true);
        mediaAssetRepository.save(asset);
        return ResponseEntity.ok(ApiResponse.success("Media asset deleted", null));
    }

    private MediaAssetResponse toResponse(MediaAsset asset) {
        String playbackUrl = liveClassPlaybackUrl(asset);
        return MediaAssetResponse.builder()
                .id(asset.getId())
                .title(asset.getTitle())
                .description(asset.getDescription())
                .mediaType(asset.getMediaType())
                // A private-bucket object URL is not playable by a browser: opening it returns
                // Backblaze's UnauthorizedAccess XML. LIVE_CLASS recordings are served only by
                // the authorised application playback route, so the storage location is never
                // serialised for them. Every other asset keeps its existing URL behaviour.
                .fileUrl(playbackUrl != null ? null : asset.getFileUrl())
                .playbackUrl(playbackUrl)
                .thumbnailUrl(asset.getThumbnailUrl())
                .durationSeconds(asset.getDurationSeconds())
                .fileSizeBytes(asset.getFileSizeBytes())
                .mimeType(asset.getMimeType())
                .status(asset.getStatus())
                .visibility(asset.getVisibility())
                .sourceType(asset.getSourceType())
                .sourceId(asset.getSourceId())
                .teacherId(asset.getTeacherId())
                .courseId(asset.getCourseId())
                .classGroupId(asset.getClassGroupId())
                .subjectId(asset.getSubjectId())
                .tags(asset.getTags())
                .createdAt(asset.getCreatedAt())
                .build();
    }

    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('TEACHER','INSTITUTION_ADMIN')")
    @Operation(summary = "Upload a media file via the media service")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadMedia(
            @RequestParam("file") MultipartFile file,
            @RequestAttribute(value = "bearerToken", required = false) String bearerToken) {
        Map<String, Object> result = mediaProxyService.uploadFile(file, bearerToken);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("File uploaded successfully", result));
    }

    @PostMapping("/presigned-upload")
    @PreAuthorize("hasAnyRole('TEACHER','INSTITUTION_ADMIN')")
    @Operation(summary = "Get a presigned URL for direct upload to object storage")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPresignedUploadUrl(
            @RequestBody Map<String, String> request,
            @RequestAttribute(value = "bearerToken", required = false) String bearerToken) {
        Map<String, Object> result = mediaProxyService.getPresignedUploadUrl(
            request.get("fileName"), request.get("contentType"), bearerToken);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{id}/download-url")
    @PreAuthorize("hasAnyRole('TEACHER','STUDENT','OTHER_LEARNER','INSTITUTION_ADMIN')")
    @Operation(summary = "Get a presigned download URL for a media asset")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDownloadUrl(
            @PathVariable UUID id,
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute(value = "institutionId", required = false) UUID institutionId,
            @RequestAttribute(value = "userRole", required = false) String userRole) {
        // The download path must never be weaker than the detail path, or the entitlement
        // check on GET /v1/media/{id} would be bypassed by asking for the file directly.
        MediaAsset asset = mediaAssetRepository.findById(id)
                .filter(a -> !Boolean.TRUE.equals(a.getIsDeleted()))
                .orElse(null);
        if (asset == null || !canViewAsset(asset, userId, institutionId, userRole)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Media asset not found"));
        }
        Map<String, Object> download = new java.util.HashMap<>();
        // Same rule as the detail view: for a live-class recording the browser must be sent to
        // the authorised application playback route, never to the private object URL. Anything
        // else keeps its existing direct-link behaviour.
        String playbackUrl = liveClassPlaybackUrl(asset);
        String url = playbackUrl != null ? playbackUrl
                : (asset.getFileUrl() != null ? asset.getFileUrl() : "");
        download.put("downloadUrl", url);
        download.put("playbackUrl", playbackUrl != null);
        download.put("assetId", asset.getId());
        return ResponseEntity.ok(ApiResponse.success(download));
    }
}
