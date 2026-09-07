package tz.elmkusoma.certificate.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.certificate.domain.TranscriptEntry;

import java.util.List;
import java.util.UUID;

@Repository
public interface TranscriptEntryRepository extends JpaRepository<TranscriptEntry, UUID> {

    @Query("SELECT te FROM TranscriptEntry te WHERE te.transcriptId = :transcriptId AND te.isDeleted = false")
    List<TranscriptEntry> findAllByTranscriptId(@Param("transcriptId") UUID transcriptId);
}
