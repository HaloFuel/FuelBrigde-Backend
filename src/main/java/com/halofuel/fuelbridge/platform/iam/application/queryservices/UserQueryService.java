package com.halofuel.fuelbridge.platform.iam.application.queryservices;

import com.halofuel.fuelbridge.platform.iam.domain.model.aggregates.User;
import com.halofuel.fuelbridge.platform.iam.domain.model.queries.GetAllUsersQuery;
import com.halofuel.fuelbridge.platform.iam.domain.model.queries.GetUserByIdQuery;
import com.halofuel.fuelbridge.platform.iam.domain.model.queries.GetUserByUsernameQuery;

import java.util.List;
import java.util.Optional;

public interface UserQueryService {
    Optional<User> handle(GetUserByIdQuery query);
    Optional<User> handle(GetUserByUsernameQuery query);
    List<User> handle(GetAllUsersQuery query);
}
