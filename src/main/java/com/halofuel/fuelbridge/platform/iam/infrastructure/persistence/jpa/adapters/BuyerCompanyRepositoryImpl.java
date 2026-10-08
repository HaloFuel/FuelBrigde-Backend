package com.halofuel.fuelbridge.platform.iam.infrastructure.persistence.jpa.adapters;

import com.halofuel.fuelbridge.platform.iam.domain.model.aggregates.BuyerCompany;
import com.halofuel.fuelbridge.platform.iam.domain.repositories.BuyerCompanyRepository;
import com.halofuel.fuelbridge.platform.iam.infrastructure.persistence.jpa.assemblers.BuyerCompanyPersistenceAssembler;
import com.halofuel.fuelbridge.platform.iam.infrastructure.persistence.jpa.repositories.BuyerCompanyPersistenceRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class BuyerCompanyRepositoryImpl implements BuyerCompanyRepository {

    private final BuyerCompanyPersistenceRepository buyerCompanyRepository;

    public BuyerCompanyRepositoryImpl(BuyerCompanyPersistenceRepository buyerCompanyRepository) {
        this.buyerCompanyRepository = buyerCompanyRepository;
    }

    @Override
    public Optional<BuyerCompany> findById(Long id) {
        return buyerCompanyRepository.findById(id)
                .map(BuyerCompanyPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public List<BuyerCompany> findAll() {
        return buyerCompanyRepository.findAll().stream()
                .map(BuyerCompanyPersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public BuyerCompany save(BuyerCompany buyerCompany) {
        var entity = BuyerCompanyPersistenceAssembler.toPersistenceFromDomain(buyerCompany);
        return BuyerCompanyPersistenceAssembler.toDomainFromPersistence(buyerCompanyRepository.save(entity));
    }
}
