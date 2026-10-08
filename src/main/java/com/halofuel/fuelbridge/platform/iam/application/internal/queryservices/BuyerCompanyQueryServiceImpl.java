package com.halofuel.fuelbridge.platform.iam.application.internal.queryservices;

import com.halofuel.fuelbridge.platform.iam.application.queryservices.BuyerCompanyQueryService;
import com.halofuel.fuelbridge.platform.iam.domain.model.aggregates.BuyerCompany;
import com.halofuel.fuelbridge.platform.iam.domain.model.queries.GetAllBuyerCompaniesQuery;
import com.halofuel.fuelbridge.platform.iam.domain.model.queries.GetBuyerCompanyByIdQuery;
import com.halofuel.fuelbridge.platform.iam.domain.repositories.BuyerCompanyRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BuyerCompanyQueryServiceImpl implements BuyerCompanyQueryService {

    private final BuyerCompanyRepository buyerCompanyRepository;

    public BuyerCompanyQueryServiceImpl(BuyerCompanyRepository buyerCompanyRepository) {
        this.buyerCompanyRepository = buyerCompanyRepository;
    }

    @Override
    public Optional<BuyerCompany> handle(GetBuyerCompanyByIdQuery query) {
        return buyerCompanyRepository.findById(query.companyId());
    }

    @Override
    public List<BuyerCompany> handle(GetAllBuyerCompaniesQuery query) {
        return buyerCompanyRepository.findAll();
    }
}
