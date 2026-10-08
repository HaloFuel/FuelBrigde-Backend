package com.halofuel.fuelbridge.platform.equipment.application.queryservices;

import com.halofuel.fuelbridge.platform.equipment.domain.model.aggregates.Equipment;
import com.halofuel.fuelbridge.platform.equipment.domain.model.queries.GetAllEquipmentQuery;
import com.halofuel.fuelbridge.platform.equipment.domain.model.queries.GetEquipmentByCompanyIdQuery;
import com.halofuel.fuelbridge.platform.equipment.domain.model.queries.GetEquipmentByIdQuery;

import java.util.List;
import java.util.Optional;

public interface EquipmentQueryService {
    Optional<Equipment> handle(GetEquipmentByIdQuery query);
    List<Equipment> handle(GetAllEquipmentQuery query);
    List<Equipment> handle(GetEquipmentByCompanyIdQuery query);
}
