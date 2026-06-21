package io.itsm.sla.infrastructure.persistence;

import io.itsm.sla.calendar.Holiday;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * JPA-сущность для праздничных дней.
 */
@Entity
@Table(name = "holiday", uniqueConstraints = @UniqueConstraint(columnNames = {"date", "zone"}))
public class HolidayEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate date;

    @Column(length = 255)
    private String name;

    @Column(nullable = false, length = 32)
    private String type;

    @Column(nullable = false, length = 64)
    private String zone = "UTC";

    public HolidayEntity() {}

    public HolidayEntity(Holiday holiday) {
        this.date = holiday.date();
        this.name = holiday.name();
        this.type = holiday.type().name();
        this.zone = holiday.zone().getId();
    }

    public Holiday toDomain() {
        return new Holiday(
            date,
            name,
            Holiday.HolidayType.valueOf(type),
            ZoneId.of(zone)
        );
    }

    // --- Getters / Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getZone() { return zone; }
    public void setZone(String zone) { this.zone = zone; }
}
