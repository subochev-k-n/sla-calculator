package io.itsm.sla.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface HolidayJpaRepository extends JpaRepository<HolidayEntity, Long> {

    List<HolidayEntity> findByDateBetween(LocalDate from, LocalDate to);

    List<HolidayEntity> findByZone(String zoneId);
}
