package com.halofuel.fuelbridge.platform.iam.application.commandservices;

import com.halofuel.fuelbridge.platform.iam.domain.model.commands.SeedRolesCommand;

public interface RoleCommandService {
    void handle(SeedRolesCommand command);
}
