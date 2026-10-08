package com.halofuel.fuelbridge.platform.iam.application.commandservices;

import com.halofuel.fuelbridge.platform.iam.domain.model.aggregates.ProviderCompany;
import com.halofuel.fuelbridge.platform.iam.domain.model.commands.CreateProviderCompanyCommand;
import com.halofuel.fuelbridge.platform.shared.application.result.ApplicationError;
import com.halofuel.fuelbridge.platform.shared.application.result.Result;

public interface ProviderCompanyCommandService {
    Result<ProviderCompany, ApplicationError> handle(CreateProviderCompanyCommand command);
}
