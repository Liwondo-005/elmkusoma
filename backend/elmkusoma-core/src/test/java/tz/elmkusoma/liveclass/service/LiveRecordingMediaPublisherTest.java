package tz.elmkusoma.liveclass.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tz.elmkusoma.course.domain.LiveClass;
import tz.elmkusoma.liveclass.domain.MediaAsset;
import tz.elmkusoma.liveclass.repository.MediaAssetRepository;
import tz.elmkusoma.teacher.domain.Teacher;
import tz.elmkusoma.teacher.repository.TeacherRepository;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The live-recording -> media library hand-off.
 *
 * <p>Covers the production gap this integration closed: a finished LiveKit recording used to be
 * only a {@code live_classes.recording_url} string with no library asset, and a failed recording
 * was log-only while leaving the {@code egress:<id>} marker in place forever.
 */
@ExtendWith(MockitoExtension.class)
class LiveRecordingMediaPublisherTest {

    @Mock private MediaAssetRepository mediaAssetRepository;
    @Mock private TeacherRepository teacherRepository;

    private LiveRecordingMediaPublisher publisher;

    private UUID institutionId;
    private UUID subjectId;
    private UUID teacherId;
    private UUID teacherUserId;
    private UUID classGroupId;
    private LiveClass liveClass;

    @BeforeEach
    void setUp() {
        publisher = new LiveRecordingMediaPublisher(mediaAssetRepository, teacherRepository);
        institutionId = UUID.randomUUID();
        subjectId = UUID.randomUUID();
        teacherId = UUID.randomUUID();
        teacherUserId = UUID.randomUUID();
        classGroupId = UUID.randomUUID();

        liveClass = new LiveClass();
        liveClass.setId(UUID.randomUUID());
        liveClass.setInstitutionId(institutionId);
        liveClass.setSubjectId(subjectId);
        liveClass.setTeacherId(teacherId);
        liveClass.setClassGroupId(classGroupId);
        liveClass.setTitle("Mathematics: Fractions");
    }

    /** LiveClass.teacherId is a teachers.id; media_assets.teacher_id stores a users.id. */
    private void stubTeacher() {
        Teacher teacher = new Teacher();
        teacher.setId(teacherId);
        teacher.setUserId(teacherUserId);
        lenient().when(teacherRepository.findById(teacherId)).thenReturn(Optional.of(teacher));
    }

    private void stubSave() {
        when(mediaAssetRepository.save(any(MediaAsset.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void publishRecording_createsReadyInstitutionScopedAsset() {
        stubTeacher();
        when(mediaAssetRepository.findFirstBySourceTypeAndSourceIdAndIsDeletedFalse(
                LiveRecordingMediaPublisher.SOURCE_LIVE_CLASS, liveClass.getId()))
                .thenReturn(Optional.empty());
        stubSave();

        Optional<MediaAsset> result = publisher.publishRecording(
                liveClass, "https://storage.example/recording.mp4", 1_800L);

        assertTrue(result.isPresent());
        MediaAsset saved = result.get();
        assertEquals("READY", saved.getStatus());
        assertEquals("INSTITUTION", saved.getVisibility());
        assertEquals("RECORDING", saved.getMediaType());
        assertEquals("LIVE_CLASS", saved.getSourceType());
        assertEquals(liveClass.getId(), saved.getSourceId());
        assertEquals(institutionId, saved.getInstitutionId(), "library asset must inherit class tenancy");
        assertEquals(subjectId, saved.getSubjectId());
        assertEquals(classGroupId, saved.getClassGroupId());
        // Resolved, not copied: media_assets.teacher_id holds a users.id.
        assertEquals(teacherUserId, saved.getTeacherId());
        assertEquals("https://storage.example/recording.mp4", saved.getFileUrl());
        assertEquals(1_800L, saved.getDurationSeconds());
        assertEquals("Mathematics: Fractions", saved.getTitle());
    }

    @Test
    void publishRecording_isIdempotentForRepeatedCompletionWebhooks() {
        MediaAsset existing = new MediaAsset();
        existing.setId(UUID.randomUUID());
        existing.setSourceType(LiveRecordingMediaPublisher.SOURCE_LIVE_CLASS);
        existing.setSourceId(liveClass.getId());
        existing.setInstitutionId(institutionId);

        when(mediaAssetRepository.findFirstBySourceTypeAndSourceIdAndIsDeletedFalse(
                LiveRecordingMediaPublisher.SOURCE_LIVE_CLASS, liveClass.getId()))
                .thenReturn(Optional.of(existing));
        stubSave();

        publisher.publishRecording(liveClass, "https://storage.example/recording.mp4", 60L);
        publisher.publishRecording(liveClass, "https://storage.example/recording.mp4", 90L);

        // Same row updated twice, never a duplicate asset.
        ArgumentCaptor<MediaAsset> captor = ArgumentCaptor.forClass(MediaAsset.class);
        verify(mediaAssetRepository, times(2)).save(captor.capture());
        assertTrue(captor.getAllValues().stream().allMatch(a -> a.getId().equals(existing.getId())));
        assertEquals(90L, captor.getAllValues().get(1).getDurationSeconds());
    }

    @Test
    void publishRecording_ignoresBlankUrlInsteadOfPublishingEmptyAsset() {
        assertTrue(publisher.publishRecording(liveClass, "   ", 60L).isEmpty());
        assertTrue(publisher.publishRecording(liveClass, null, 60L).isEmpty());

        verify(mediaAssetRepository, never()).save(any(MediaAsset.class));
    }

    @Test
    void publishRecording_refusesEgressMarkerBecauseItIsNotAPlayableUrl() {
        // The transient egress:<id> marker is LiveKit bookkeeping. Publishing it would put a
        // non-playable entry in a learner-facing library.
        assertTrue(publisher.publishRecording(liveClass, "egress:RM_xYz123", 60L).isEmpty());

        verify(mediaAssetRepository, never()).save(any(MediaAsset.class));
    }

    @Test
    void markRecordingFailed_createsQueryableFailedAssetWithReason() {
        stubTeacher();
        when(mediaAssetRepository.findFirstBySourceTypeAndSourceIdAndIsDeletedFalse(
                LiveRecordingMediaPublisher.SOURCE_LIVE_CLASS, liveClass.getId()))
                .thenReturn(Optional.empty());
        stubSave();

        Optional<MediaAsset> result = publisher.markRecordingFailed(liveClass, "egress RTMP_ERROR");

        assertTrue(result.isPresent());
        MediaAsset failed = result.get();
        assertEquals("FAILED", failed.getStatus());
        assertEquals(institutionId, failed.getInstitutionId());
        assertNotNull(failed.getDescription());
        assertTrue(failed.getDescription().contains("egress RTMP_ERROR"),
                "failure reason must be visible in the library, not only in the log");
        assertFalse("READY".equals(failed.getStatus()));
    }

    @Test
    void markRecordingFailed_downgradesExistingAssetInsteadOfDuplicating() {
        MediaAsset existing = new MediaAsset();
        existing.setId(UUID.randomUUID());
        existing.setStatus("READY");
        existing.setFileUrl("https://storage.example/partial.mp4");
        existing.setInstitutionId(institutionId);

        when(mediaAssetRepository.findFirstBySourceTypeAndSourceIdAndIsDeletedFalse(
                LiveRecordingMediaPublisher.SOURCE_LIVE_CLASS, liveClass.getId()))
                .thenReturn(Optional.of(existing));
        stubSave();

        MediaAsset failed = publisher.markRecordingFailed(liveClass, null).orElseThrow();

        assertEquals(existing.getId(), failed.getId());
        assertEquals("FAILED", failed.getStatus());
        verify(mediaAssetRepository, times(1)).save(any(MediaAsset.class));
    }

    @Test
    void findForLiveClass_returnsLibraryAssetKeyedBySource() {
        MediaAsset asset = new MediaAsset();
        asset.setId(UUID.randomUUID());
        when(mediaAssetRepository.findFirstBySourceTypeAndSourceIdAndIsDeletedFalse(
                LiveRecordingMediaPublisher.SOURCE_LIVE_CLASS, liveClass.getId()))
                .thenReturn(Optional.of(asset));

        assertTrue(publisher.findForLiveClass(liveClass.getId()).isPresent());
        assertTrue(publisher.findForLiveClass(null).isEmpty());
    }
}
