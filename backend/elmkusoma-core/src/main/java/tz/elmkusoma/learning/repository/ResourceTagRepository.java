package tz.elmkusoma.learning.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.learning.domain.ResourceTag;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResourceTagRepository extends JpaRepository<ResourceTag, UUID> {

    Optional<ResourceTag> findByName(String name);

    List<ResourceTag> findByIsSystemTrue();

    @Query("SELECT t FROM ResourceTag t WHERE t.name ILIKE %:name%")
    List<ResourceTag> searchByName(@Param("name") String name);
}