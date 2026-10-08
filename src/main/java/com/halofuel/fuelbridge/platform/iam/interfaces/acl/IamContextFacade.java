package com.halofuel.fuelbridge.platform.iam.interfaces.acl;

import com.halofuel.fuelbridge.platform.iam.application.queryservices.BuyerCompanyQueryService;
import com.halofuel.fuelbridge.platform.iam.application.queryservices.ProviderCompanyQueryService;
import com.halofuel.fuelbridge.platform.iam.application.queryservices.UserQueryService;
import com.halofuel.fuelbridge.platform.iam.domain.model.aggregates.BuyerCompany;
import com.halofuel.fuelbridge.platform.iam.domain.model.aggregates.ProviderCompany;
import com.halofuel.fuelbridge.platform.iam.domain.model.aggregates.User;
import com.halofuel.fuelbridge.platform.iam.domain.model.queries.GetBuyerCompanyByIdQuery;
import com.halofuel.fuelbridge.platform.iam.domain.model.queries.GetProviderCompanyByIdQuery;
import com.halofuel.fuelbridge.platform.iam.domain.model.queries.GetUserByIdQuery;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class IamContextFacade {

    private final UserQueryService userQueryService;
    private final BuyerCompanyQueryService buyerCompanyQueryService;
    private final ProviderCompanyQueryService providerCompanyQueryService;

    public IamContextFacade(UserQueryService userQueryService,
                            BuyerCompanyQueryService buyerCompanyQueryService,
                            ProviderCompanyQueryService providerCompanyQueryService) {
        this.userQueryService = userQueryService;
        this.buyerCompanyQueryService = buyerCompanyQueryService;
        this.providerCompanyQueryService = providerCompanyQueryService;
    }

    public Optional<User> fetchUserById(Long userId) {
        return userQueryService.handle(new GetUserByIdQuery(userId));
    }

    public Optional<BuyerCompany> fetchBuyerCompanyById(Long companyId) {
        return buyerCompanyQueryService.handle(new GetBuyerCompanyByIdQuery(companyId));
    }

    public Optional<ProviderCompany> fetchProviderCompanyById(Long providerId) {
        return providerCompanyQueryService.handle(new GetProviderCompanyByIdQuery(providerId));
    }
}
