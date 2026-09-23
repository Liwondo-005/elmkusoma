package tz.elmkusoma.administration.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.administration.domain.ContentReport;

import java.util.UUID;

@Repository
public interface ContentReportRepository extends JpaRepository<ContentReport, UUID> {
    Page<ContentReport> findByIsDeletedFalse(Pageable pageable);
    Page<ContentReport> findByStatusAndIsDeletedFalse(String status, Pageable pageable);
}
