package com.halofuel.fuelbridge.platform.inventory.application.queryservices;

import com.halofuel.fuelbridge.platform.inventory.domain.model.aggregates.FuelProduct;
import com.halofuel.fuelbridge.platform.inventory.domain.model.queries.GetAllFuelProductsQuery;
import com.halofuel.fuelbridge.platform.inventory.domain.model.queries.GetFuelProductByIdQuery;
import com.halofuel.fuelbridge.platform.inventory.domain.model.queries.GetFuelProductsByProviderIdQuery;

import java.util.List;
import java.util.Optional;

public interface FuelProductQueryService {
    Optional<FuelProduct> handle(GetFuelProductByIdQuery query);
    List<FuelProduct> handle(GetAllFuelProductsQuery query);
    List<FuelProduct> handle(GetFuelProductsByProviderIdQuery query);
}

