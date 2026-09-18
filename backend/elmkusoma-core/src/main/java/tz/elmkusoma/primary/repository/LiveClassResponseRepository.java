package tz.elmkusoma.primary.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.primary.domain.LiveClassResponse;

import java.util.List;
import java.util.UUID;

@Repository
public interface LiveClassResponseRepository extends JpaRepository<LiveClassResponse, UUID> {

    List<LiveClassResponse> findByLiveClassIdAndStudentIdAndIsDeletedFalse(UUID liveClassId, UUID studentId);
}
