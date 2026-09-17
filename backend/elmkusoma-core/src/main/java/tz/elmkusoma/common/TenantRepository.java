package tz.elmkusoma.common;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.UUID;

@NoRepositoryBean
public interface TenantRepository<T, ID> extends JpaRepository<T, ID> {

    List<T> findByInstitutionId(UUID institutionId);

    long countByInstitutionId(UUID institutionId);
}
