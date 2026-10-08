package com.halofuel.fuelbridge.platform.iam.application.internal.commandservices;

import com.halofuel.fuelbridge.platform.iam.application.commandservices.BuyerCompanyCommandService;
import com.halofuel.fuelbridge.platform.iam.domain.model.aggregates.BuyerCompany;
import com.halofuel.fuelbridge.platform.iam.domain.model.commands.CreateBuyerCompanyCommand;
import com.halofuel.fuelbridge.platform.iam.domain.repositories.BuyerCompanyRepository;
import com.halofuel.fuelbridge.platform.shared.application.result.ApplicationError;
import com.halofuel.fuelbridge.platform.shared.application.result.Result;
import org.springframework.stereotype.Service;

@Service
public class BuyerCompanyCommandServiceImpl implements BuyerCompanyCommandService {

    private final BuyerCompanyRepository buyerCompanyRepository;

    public BuyerCompanyCommandServiceImpl(BuyerCompanyRepository buyerCompanyRepository) {
        this.buyerCompanyRepository = buyerCompanyRepository;
    }

    @Override
    public Result<BuyerCompany, ApplicationError> handle(CreateBuyerCompanyCommand command) {
        try {
            var buyerCompany = new BuyerCompany(command);
            var saved = buyerCompanyRepository.save(buyerCompany);
            return Result.success(saved);
        } catch (Exception e) {
            return Result.failure(ApplicationError.unexpected("BuyerCompanyCommandService", e.getMessage()));
        }
    }
}
