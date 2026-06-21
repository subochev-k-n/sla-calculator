package io.itsm.sla.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SLARuleRepository extends JpaRepository<SLARuleEntity, String> {

    List<SLARuleEntity> findByActiveTrueOrderByPriorityAsc();
}
