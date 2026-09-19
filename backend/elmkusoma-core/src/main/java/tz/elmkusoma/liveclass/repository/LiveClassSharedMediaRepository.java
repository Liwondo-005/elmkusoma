package tz.elmkusoma.liveclass.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.liveclass.domain.LiveClassSharedMedia;

import java.util.List;
import java.util.UUID;

@Repository
public interface LiveClassSharedMediaRepository extends JpaRepository<LiveClassSharedMedia, UUID> {
    List<LiveClassSharedMedia> findByLiveClassIdAndIsDeletedFalse(UUID liveClassId);
}
