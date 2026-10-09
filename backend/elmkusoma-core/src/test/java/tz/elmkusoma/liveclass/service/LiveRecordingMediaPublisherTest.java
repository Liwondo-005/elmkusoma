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

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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

    private LiveRecordingMediaPublisher publisher;

    private UUID institutionId;
    private UUID subjectId;
    private UUID teacherId;
    private LiveClass liveClass;

    @BeforeEach
    void setUp() {
        publisher = new LiveRecordingMediaPublisher(mediaAssetRepository);
        institutionId = UUID.randomUUID();
        subjectId = UUID.randomUUID();
        teacherId = UUID.randomUUID();

        liveClass = new LiveClass();
        liveClass.setId(UUID.randomUUID());
        liveClass.setInstitutionId(institutionId);
        liveClass.setSubjectId(subjectId);
        liveClass.setTeacherId(teacherId);
        liveClass.setTitle("Mathematics: Fractions");
    }

    private void stubSave() {
        when(mediaAssetRepository.save(any(MediaAsset.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void publishRecording_createsReadyInstitutionScopedAsset() {
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
        assertEquals(teacherId, saved.getTeacherId());
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
    void markRecordingFailed_createsQueryableFailedAssetWithReason() {
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
