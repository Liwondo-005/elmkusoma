package tz.elmkusoma.shared.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.shared.domain.LegalDocument;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LegalDocumentRepository extends JpaRepository<LegalDocument, UUID> {

    Optional<LegalDocument> findByDocTypeAndIsDeletedFalse(String docType);

    java.util.List<LegalDocument> findAllByIsDeletedFalseOrderByDocTypeAsc();
}