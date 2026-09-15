package tz.elmkusoma.course.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.course.domain.LiveClassParticipant;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LiveClassParticipantRepository extends JpaRepository<LiveClassParticipant, UUID> {

    Optional<LiveClassParticipant> findByLiveClassIdAndStudentIdAndIsDeletedFalse(UUID liveClassId, UUID studentId);

    List<LiveClassParticipant> findByLiveClassIdAndIsDeletedFalse(UUID liveClassId);

    List<LiveClassParticipant> findByStudentIdAndIsDeletedFalse(UUID studentId);

    @Query("SELECT COUNT(p) FROM LiveClassParticipant p WHERE p.liveClassId = :liveClassId AND p.isDeleted = false")
    long countByLiveClassId(@Param("liveClassId") UUID liveClassId);

    @Query("SELECT COUNT(p) FROM LiveClassParticipant p WHERE p.liveClassId = :liveClassId AND p.attendanceStatus = 'COMPLETED' AND p.isDeleted = false")
    long countCompletedByLiveClassId(@Param("liveClassId") UUID liveClassId);
}
