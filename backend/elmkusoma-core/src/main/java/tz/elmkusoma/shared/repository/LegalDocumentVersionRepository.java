package tz.elmkusoma.shared.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.shared.domain.LegalDocumentVersion;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LegalDocumentVersionRepository extends JpaRepository<LegalDocumentVersion, UUID> {

    List<LegalDocumentVersion> findByLegalDocumentIdAndIsDeletedFalseOrderByVersionDesc(UUID legalDocumentId);

    Optional<LegalDocumentVersion> findByLegalDocumentIdAndVersionAndIsDeletedFalse(UUID legalDocumentId, Integer version);

    /** The single version the public endpoint is allowed to return. */
    Optional<LegalDocumentVersion> findTopByLegalDocumentIdAndVersionAndIsDeletedFalse(UUID legalDocumentId, Integer version);
}