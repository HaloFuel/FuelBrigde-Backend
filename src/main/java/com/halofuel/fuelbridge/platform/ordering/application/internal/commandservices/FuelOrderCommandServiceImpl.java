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
        var totalPrice = product.getPricePerUnit() * command.requestedQuantity();
        var order = new FuelOrder(command, totalPrice);
        var saved = fuelOrderRepository.save(order);
        return Result.success(saved);
    }

    @Override
    public Result<FuelOrder, ApplicationError> handle(ConfirmFuelOrderCommand command) {
        var existing = fuelOrderRepository.findById(command.orderId());
        if (existing.isEmpty()) {
            return Result.failure(ApplicationError.notFound("FuelOrder", command.orderId().toString()));
        }
        var order = existing.get();
        order.confirm();
        return Result.success(fuelOrderRepository.save(order));
    }

    @Override
    public Result<FuelOrder, ApplicationError> handle(CancelFuelOrderCommand command) {
        var existing = fuelOrderRepository.findById(command.orderId());
        if (existing.isEmpty()) {
            return Result.failure(ApplicationError.notFound("FuelOrder", command.orderId().toString()));
        }
        var order = existing.get();
        order.cancel();
        return Result.success(fuelOrderRepository.save(order));
    }

    @Override
    public Result<FuelOrder, ApplicationError> handle(DispatchFuelOrderCommand command) {
        var existing = fuelOrderRepository.findById(command.orderId());
        if (existing.isEmpty()) {
            return Result.failure(ApplicationError.notFound("FuelOrder", command.orderId().toString()));
        }
        var order = existing.get();
        order.dispatch();
        return Result.success(fuelOrderRepository.save(order));
    }
}
