package com.halofuel.fuelbridge.platform.iam.interfaces.rest.transform;

import com.halofuel.fuelbridge.platform.iam.domain.model.commands.CreateBuyerCompanyCommand;
import com.halofuel.fuelbridge.platform.iam.interfaces.rest.resources.CreateBuyerCompanyResource;

public final class CreateBuyerCompanyCommandFromResourceAssembler {

    private CreateBuyerCompanyCommandFromResourceAssembler() {
    }

    public static CreateBuyerCompanyCommand toCommandFromResource(CreateBuyerCompanyResource resource) {
        return new CreateBuyerCompanyCommand(resource.name(), resource.ruc(), resource.sector(),
                resource.address(), resource.contactEmail(), resource.phone());
    }
}
