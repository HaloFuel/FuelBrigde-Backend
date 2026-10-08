package com.halofuel.fuelbridge.platform.ordering.infrastructure.persistence.jpa.adapters;

import com.halofuel.fuelbridge.platform.ordering.domain.model.aggregates.FuelOrder;
import com.halofuel.fuelbridge.platform.ordering.domain.repositories.FuelOrderRepository;
import com.halofuel.fuelbridge.platform.ordering.infrastructure.persistence.jpa.assemblers.FuelOrderPersistenceAssembler;
import com.halofuel.fuelbridge.platform.ordering.infrastructure.persistence.jpa.repositories.FuelOrderPersistenceRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class FuelOrderRepositoryImpl implements FuelOrderRepository {

    private final FuelOrderPersistenceRepository fuelOrderPersistenceRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    public FuelOrderRepositoryImpl(FuelOrderPersistenceRepository fuelOrderPersistenceRepository,
                                   ApplicationEventPublisher applicationEventPublisher) {
        this.fuelOrderPersistenceRepository = fuelOrderPersistenceRepository;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public Optional<FuelOrder> findById(Long id) {
        return fuelOrderPersistenceRepository.findById(id)
                .map(FuelOrderPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public List<FuelOrder> findAll() {
        return fuelOrderPersistenceRepository.findAll().stream()
                .map(FuelOrderPersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public List<FuelOrder> findByCompanyId(Long companyId) {
        return fuelOrderPersistenceRepository.findByCompanyId(companyId).stream()
                .map(FuelOrderPersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public List<FuelOrder> findByProviderId(Long providerId) {
        return fuelOrderPersistenceRepository.findByProviderId(providerId).stream()
                .map(FuelOrderPersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public FuelOrder save(FuelOrder fuelOrder) {
        var entity = FuelOrderPersistenceAssembler.toPersistenceFromDomain(fuelOrder);
        var savedEntity = fuelOrderPersistenceRepository.save(entity);
        fuelOrder.domainEvents().forEach(applicationEventPublisher::publishEvent);
        fuelOrder.clearDomainEvents();
        return FuelOrderPersistenceAssembler.toDomainFromPersistence(savedEntity);
    }
}
