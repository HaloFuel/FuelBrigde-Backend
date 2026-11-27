package com.halofuel.fuelbridge.platform.notification.infrastructure.persistence.jpa.repositories;

import com.halofuel.fuelbridge.platform.notification.infrastructure.persistence.jpa.entities.ProcessedEventRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRecordRepository extends JpaRepository<ProcessedEventRecord, Long> {
    boolean existsByEventId(String eventId);
}
