package com.halofuel.fuelbridge.platform.ordering.application.queryservices;

import com.halofuel.fuelbridge.platform.ordering.domain.model.aggregates.FuelOrder;
import com.halofuel.fuelbridge.platform.ordering.domain.model.queries.GetAllFuelOrdersQuery;
import com.halofuel.fuelbridge.platform.ordering.domain.model.queries.GetFuelOrderByIdQuery;
import com.halofuel.fuelbridge.platform.ordering.domain.model.queries.GetFuelOrdersByCompanyIdQuery;
import com.halofuel.fuelbridge.platform.ordering.domain.model.queries.GetFuelOrdersByProviderIdQuery;

import java.util.List;
import java.util.Optional;

public interface FuelOrderQueryService {
    Optional<FuelOrder> handle(GetFuelOrderByIdQuery query);
    List<FuelOrder> handle(GetAllFuelOrdersQuery query);
    List<FuelOrder> handle(GetFuelOrdersByCompanyIdQuery query);
    List<FuelOrder> handle(GetFuelOrdersByProviderIdQuery query);
}
