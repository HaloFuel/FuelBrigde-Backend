package com.halofuel.fuelbridge.platform.inventory.application.internal.commandservices;

import com.halofuel.fuelbridge.platform.inventory.application.commandservices.FuelProductCommandService;
import com.halofuel.fuelbridge.platform.inventory.domain.model.aggregates.FuelProduct;
import com.halofuel.fuelbridge.platform.inventory.domain.model.commands.CreateFuelProductCommand;
import com.halofuel.fuelbridge.platform.inventory.domain.model.commands.DeleteFuelProductCommand;
import com.halofuel.fuelbridge.platform.inventory.domain.model.commands.ReleaseFuelStockCommand;
import com.halofuel.fuelbridge.platform.inventory.domain.model.commands.ReserveFuelStockCommand;
import com.halofuel.fuelbridge.platform.inventory.domain.model.commands.UpdateFuelProductCommand;
import com.halofuel.fuelbridge.platform.inventory.domain.model.commands.UpdateFuelProductStockCommand;
import com.halofuel.fuelbridge.platform.inventory.domain.repositories.FuelProductRepository;
import com.halofuel.fuelbridge.platform.shared.application.result.ApplicationError;
import com.halofuel.fuelbridge.platform.shared.application.result.Result;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.PersistenceException;

@Service
public class FuelProductCommandServiceImpl implements FuelProductCommandService {

    private final FuelProductRepository fuelProductRepository;

    public FuelProductCommandServiceImpl(FuelProductRepository fuelProductRepository) {
        this.fuelProductRepository = fuelProductRepository;
    }

    @Override
    public Result<FuelProduct, ApplicationError> handle(CreateFuelProductCommand command) {
        var fuelProduct = new FuelProduct(command);
        var saved = fuelProductRepository.save(fuelProduct);
        return Result.success(saved);
    }

    @Override
    public Result<FuelProduct, ApplicationError> handle(UpdateFuelProductStockCommand command) {
        var existing = fuelProductRepository.findById(command.fuelProductId());
        if (existing.isEmpty()) {
            return Result.failure(ApplicationError.notFound("FuelProduct", command.fuelProductId().toString()));
        }
        var fuelProduct = existing.get();
        fuelProduct.updateStock(command.newStock());
        var saved = fuelProductRepository.save(fuelProduct);
        return Result.success(saved);
    }

    @Override
    public Result<FuelProduct, ApplicationError> handle(UpdateFuelProductCommand command) {
        var existing = fuelProductRepository.findById(command.fuelProductId());
        if (existing.isEmpty()) {
            return Result.failure(ApplicationError.notFound("FuelProduct", command.fuelProductId().toString()));
        }
        var fuelProduct = existing.get();
        fuelProduct.update(command);
        var saved = fuelProductRepository.save(fuelProduct);
        return Result.success(saved);
    }

    @Override
    public Result<Long, ApplicationError> handle(DeleteFuelProductCommand command) {
        if (!fuelProductRepository.existsById(command.fuelProductId())) {
            return Result.failure(ApplicationError.notFound("FuelProduct", command.fuelProductId().toString()));
        }
        try {
            fuelProductRepository.deleteById(command.fuelProductId());
            return Result.success(command.fuelProductId());
        } catch (DataIntegrityViolationException | PersistenceException exception) {
            return Result.failure(ApplicationError.conflict(
                    "FuelProduct",
                    "Cannot delete a fuel product referenced by existing requests or orders"));
        }
    }

    @Override
    @Transactional
    public Result<FuelProduct, ApplicationError> handle(ReserveFuelStockCommand command) {
        if (!isPositive(command.quantity())) {
            return Result.failure(ApplicationError.validationError("quantity",
                    "La cantidad debe ser un número válido mayor que cero."));
        }
        var existing = fuelProductRepository.findById(command.fuelProductId());
        if (existing.isEmpty()) {
            return Result.failure(ApplicationError.notFound("FuelProduct", command.fuelProductId().toString()));
        }
        var fuelProduct = existing.get();
        if (!fuelProduct.reserveStock(command.quantity())) {
            return Result.failure(ApplicationError.conflict("FuelProduct", "Insufficient inventory stock"));
        }
        return Result.success(fuelProductRepository.save(fuelProduct));
    }

    @Override
    @Transactional
    public Result<FuelProduct, ApplicationError> handle(ReleaseFuelStockCommand command) {
        if (!isPositive(command.quantity())) {
            return Result.failure(ApplicationError.validationError("quantity",
                    "La cantidad debe ser un número válido mayor que cero."));
        }
        var existing = fuelProductRepository.findById(command.fuelProductId());
        if (existing.isEmpty()) {
            return Result.failure(ApplicationError.notFound("FuelProduct", command.fuelProductId().toString()));
        }
        var fuelProduct = existing.get();
        fuelProduct.releaseStock(command.quantity());
        return Result.success(fuelProductRepository.save(fuelProduct));
    }

    private static boolean isPositive(Double quantity) {
        return quantity != null && Double.isFinite(quantity) && quantity > 0;
    }
}
