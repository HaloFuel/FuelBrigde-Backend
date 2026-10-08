package com.halofuel.fuelbridge.platform.iam.interfaces.rest.transform;

import com.halofuel.fuelbridge.platform.iam.domain.model.aggregates.BuyerCompany;
import com.halofuel.fuelbridge.platform.iam.interfaces.rest.resources.BuyerCompanyResource;

public final class BuyerCompanyResourceFromEntityAssembler {

    private BuyerCompanyResourceFromEntityAssembler() {
    }

    public static BuyerCompanyResource toResourceFromEntity(BuyerCompany buyerCompany) {
        return new BuyerCompanyResource(buyerCompany.getId(), buyerCompany.getName(),
                buyerCompany.getRuc(), buyerCompany.getSector(), buyerCompany.getAddress(),
                buyerCompany.getContactEmail(), buyerCompany.getPhone());
    }
}
