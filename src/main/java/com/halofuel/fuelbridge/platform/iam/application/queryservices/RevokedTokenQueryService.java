package com.halofuel.fuelbridge.platform.iam.application.queryservices;

import com.halofuel.fuelbridge.platform.iam.domain.model.queries.IsTokenRevokedQuery;

public interface RevokedTokenQueryService {
    boolean handle(IsTokenRevokedQuery query);
}
