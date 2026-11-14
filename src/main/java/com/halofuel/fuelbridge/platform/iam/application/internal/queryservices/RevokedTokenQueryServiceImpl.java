package com.halofuel.fuelbridge.platform.iam.application.internal.queryservices;

import com.halofuel.fuelbridge.platform.iam.application.queryservices.RevokedTokenQueryService;
import com.halofuel.fuelbridge.platform.iam.domain.model.aggregates.RevokedToken;
import com.halofuel.fuelbridge.platform.iam.domain.model.queries.IsTokenRevokedQuery;
import com.halofuel.fuelbridge.platform.iam.domain.repositories.RevokedTokenRepository;
import org.springframework.stereotype.Service;

@Service
public class RevokedTokenQueryServiceImpl implements RevokedTokenQueryService {

    private final RevokedTokenRepository revokedTokenRepository;

    public RevokedTokenQueryServiceImpl(RevokedTokenRepository revokedTokenRepository) {
        this.revokedTokenRepository = revokedTokenRepository;
    }

    @Override
    public boolean handle(IsTokenRevokedQuery query) {
        return revokedTokenRepository.existsByTokenHash(RevokedToken.hash(query.token()));
    }
}
