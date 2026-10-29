package com.halofuel.fuelbridge.platform.ordering.application.internal.commandservices;

import com.halofuel.fuelbridge.platform.inventory.application.queryservices.FuelProductQueryService;
import com.halofuel.fuelbridge.platform.inventory.domain.model.queries.GetFuelProductByIdQuery;
import com.halofuel.fuelbridge.platform.ordering.application.commandservices.FuelOrderCommandService;
import com.halofuel.fuelbridge.platform.ordering.domain.model.aggregates.FuelOrder;
import com.halofuel.fuelbridge.platform.ordering.domain.model.commands.CancelFuelOrderCommand;
import com.halofuel.fuelbridge.platform.ordering.domain.model.commands.ConfirmFuelOrderCommand;
import com.halofuel.fuelbridge.platform.ordering.domain.model.commands.CreateFuelOrderCommand;
import com.halofuel.fuelbridge.platform.ordering.domain.model.commands.DispatchFuelOrderCommand;
import com.halofuel.fuelbridge.platform.ordering.domain.repositories.FuelOrderRepository;
import com.halofuel.fuelbridge.platform.shared.application.result.ApplicationError;
import com.halofuel.fuelbridge.platform.shared.application.result.Result;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

/**
 * Coordinates order commands and persistence. Status changes and their domain
 * events belong to the aggregate; handlers invoke its methods before saving.
 * Confirmation and cancellation currently have no source-status guard, while
 * dispatch requires PENDING and rejects other states with IllegalStateException.
 */
@Service
public class FuelOrderCommandServiceImpl implements FuelOrderCommandService {

    private final FuelOrderRepository fuelOrderRepository;
    private final FuelProductQueryService fuelProductQueryService;

    public FuelOrderCommandServiceImpl(FuelOrderRepository fuelOrderRepository,
                                       FuelProductQueryService fuelProductQueryService) {
        this.fuelOrderRepository = fuelOrderRepository;
        this.fuelProductQueryService = fuelProductQueryService;
    }

    @Override
    public Result<FuelOrder, ApplicationError> handle(CreateFuelOrderCommand command) {
        var productResult = fuelProductQueryService.handle(new GetFuelProductByIdQuery(command.fuelProductId()));
        if (productResult.isEmpty()) {
            return Result.failure(ApplicationError.notFound("FuelProduct", command.fuelProductId().toString()));
        }
        var product = productResult.get();
        // Capture the catalog price at creation so later price changes do not
        // recalculate the amount stored on this order.
        var totalPrice = product.getPricePerUnit() * command.requestedQuantity();
        var order = new FuelOrder(command, totalPrice);
        var saved = fuelOrderRepository.save(order);
        return Result.success(saved);
    }

    @Override
    public Result<FuelOrder, ApplicationError> handle(ConfirmFuelOrderCommand command) {
        return updateOrder(command.orderId(), FuelOrder::confirm);
    }

    @Override
    public Result<FuelOrder, ApplicationError> handle(CancelFuelOrderCommand command) {
        return updateOrder(command.orderId(), FuelOrder::cancel);
    }

    @Override
    public Result<FuelOrder, ApplicationError> handle(DispatchFuelOrderCommand command) {
        return updateOrder(command.orderId(), FuelOrder::dispatch);
    }

    private Result<FuelOrder, ApplicationError> updateOrder(Long orderId, Consumer<FuelOrder> transition) {
        var existing = fuelOrderRepository.findById(orderId);
        if (existing.isEmpty()) {
            return Result.failure(ApplicationError.notFound("FuelOrder", orderId.toString()));
        }
        var order = existing.get();
        transition.accept(order);
        return Result.success(fuelOrderRepository.save(order));
    }
}
