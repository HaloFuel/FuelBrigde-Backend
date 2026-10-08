package com.halofuel.fuelbridge.platform.iam.application.internal.commandservices;

import com.halofuel.fuelbridge.platform.iam.application.commandservices.ProviderCompanyCommandService;
import com.halofuel.fuelbridge.platform.iam.domain.model.aggregates.ProviderCompany;
import com.halofuel.fuelbridge.platform.iam.domain.model.commands.CreateProviderCompanyCommand;
import com.halofuel.fuelbridge.platform.iam.domain.repositories.ProviderCompanyRepository;
import com.halofuel.fuelbridge.platform.shared.application.result.ApplicationError;
import com.halofuel.fuelbridge.platform.shared.application.result.Result;
import org.springframework.stereotype.Service;

@Service
public class ProviderCompanyCommandServiceImpl implements ProviderCompanyCommandService {

    private final ProviderCompanyRepository providerCompanyRepository;

    public ProviderCompanyCommandServiceImpl(ProviderCompanyRepository providerCompanyRepository) {
        this.providerCompanyRepository = providerCompanyRepository;
    }

    @Override
    public Result<ProviderCompany, ApplicationError> handle(CreateProviderCompanyCommand command) {
        try {
            var providerCompany = new ProviderCompany(command);
            var saved = providerCompanyRepository.save(providerCompany);
            return Result.success(saved);
        } catch (Exception e) {
            return Result.failure(ApplicationError.unexpected("ProviderCompanyCommandService", e.getMessage()));
        }
    }
}
