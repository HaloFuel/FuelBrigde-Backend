package com.halofuel.fuelbridge.platform.ordering.infrastructure.persistence.jpa.repositories;

import com.halofuel.fuelbridge.platform.ordering.infrastructure.persistence.jpa.entities.OutboxEventPersistenceEntity;
import com.halofuel.fuelbridge.platform.ordering.infrastructure.persistence.jpa.entities.OutboxEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface OutboxEventPersistenceRepository extends JpaRepository<OutboxEventPersistenceEntity, Long> {

    /** Next batch the relay delivers, oldest first so the events of an order keep their order. */
    List<OutboxEventPersistenceEntity> findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByIdAsc(
            OutboxEventStatus status, Instant now);

    List<OutboxEventPersistenceEntity> findByStatus(OutboxEventStatus status);
}
