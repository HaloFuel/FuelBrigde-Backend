package com.halofuel.fuelbridge.platform.inventory.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import com.halofuel.fuelbridge.platform.inventory.application.commandservices.FuelProductCommandService;
import com.halofuel.fuelbridge.platform.inventory.application.queryservices.FuelProductQueryService;
import com.halofuel.fuelbridge.platform.inventory.domain.model.commands.DeleteFuelProductCommand;
import com.halofuel.fuelbridge.platform.inventory.domain.model.queries.GetAllFuelProductsQuery;
import com.halofuel.fuelbridge.platform.inventory.domain.model.queries.GetFuelProductByIdQuery;
import com.halofuel.fuelbridge.platform.inventory.domain.model.queries.GetFuelProductsByProviderIdQuery;
import com.halofuel.fuelbridge.platform.inventory.interfaces.rest.resources.CreateFuelProductResource;
import com.halofuel.fuelbridge.platform.inventory.interfaces.rest.resources.FuelProductResource;
import com.halofuel.fuelbridge.platform.inventory.interfaces.rest.resources.UpdateFuelProductResource;
import com.halofuel.fuelbridge.platform.inventory.interfaces.rest.resources.UpdateFuelProductStockResource;
import com.halofuel.fuelbridge.platform.inventory.interfaces.rest.transform.CreateFuelProductCommandFromResourceAssembler;
import com.halofuel.fuelbridge.platform.inventory.interfaces.rest.transform.FuelProductResourceFromEntityAssembler;
import com.halofuel.fuelbridge.platform.inventory.interfaces.rest.transform.UpdateFuelProductCommandFromResourceAssembler;
import com.halofuel.fuelbridge.platform.inventory.interfaces.rest.transform.UpdateFuelProductStockCommandFromResourceAssembler;
import com.halofuel.fuelbridge.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/fuel-products", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Fuel Products", description = "Inventory management endpoints")
public class FuelProductsController {

    private final FuelProductCommandService fuelProductCommandService;
    private final FuelProductQueryService fuelProductQueryService;

    public FuelProductsController(FuelProductCommandService fuelProductCommandService,
                                  FuelProductQueryService fuelProductQueryService) {
        this.fuelProductCommandService = fuelProductCommandService;
        this.fuelProductQueryService = fuelProductQueryService;
    }

    @Operation(
        summary = "Registrar producto de combustible",
        description = "Crea un producto del proveedor con precio por unidad, stock disponible y capacidad de almacenamiento.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true, description = "Datos necesarios para registrar producto de combustible.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.halofuel.fuelbridge.platform.inventory.interfaces.rest.resources.CreateFuelProductResource.class),
                examples = { @ExampleObject(name = "principal", value = "{\"name\":\"Diesel B5 S-50\",\"fuelType\":\"DIESEL\",\"pricePerUnit\":4.8,\"unit\":\"L\",\"availableStock\":18000,\"capacity\":25000,\"providerId\":202,\"active\":true}") }))
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Registro creado correctamente.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.halofuel.fuelbridge.platform.inventory.interfaces.rest.resources.FuelProductResource.class), examples = @ExampleObject(name = "respuesta", value = "{\"id\":303,\"name\":\"Diesel B5 S-50\",\"fuelType\":\"DIESEL\",\"pricePerUnit\":4.8,\"unit\":\"L\",\"availableStock\":18000,\"capacity\":25000,\"providerId\":202,\"active\":true}"))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida: revisar identificadores, campos o valores indicados en el cuerpo o los parámetros.", content = @Content),
        @ApiResponse(responseCode = "401", description = "Falta un JWT Bearer válido, o el token ha expirado o tiene una firma incorrecta.", content = @Content),
        @ApiResponse(responseCode = "403", description = "Solicitud rechazada por la política de acceso o por un origen CORS no permitido.", content = @Content),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content)
    })
    @PostMapping
    public ResponseEntity<?> createFuelProduct(@RequestBody CreateFuelProductResource resource) {
        var command = CreateFuelProductCommandFromResourceAssembler.toCommandFromResource(resource);
        var result = fuelProductCommandService.handle(command);
        return ResponseEntityAssembler.toResponseEntityFromResult(
                result,
                FuelProductResourceFromEntityAssembler::toResourceFromEntity,
                HttpStatus.CREATED);
    }

    @Operation(
        summary = "Actualizar stock de combustible",
        description = "Reemplaza el stock disponible del producto por la cantidad indicada en newStock.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = {
            @Parameter(name = "fuelProductId", in = ParameterIn.PATH, required = true, description = "Identificador del producto de combustible.", example = "303")
        },
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true, description = "Datos necesarios para actualizar stock de combustible.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.halofuel.fuelbridge.platform.inventory.interfaces.rest.resources.UpdateFuelProductStockResource.class),
                examples = { @ExampleObject(name = "principal", value = "{\"newStock\":22000}") }))
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operación completada correctamente.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.halofuel.fuelbridge.platform.inventory.interfaces.rest.resources.FuelProductResource.class), examples = @ExampleObject(name = "respuesta", value = "{\"id\":303,\"name\":\"Diesel B5 S-50\",\"fuelType\":\"DIESEL\",\"pricePerUnit\":4.8,\"unit\":\"L\",\"availableStock\":22000,\"capacity\":25000,\"providerId\":202,\"active\":true}"))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida: revisar identificadores, campos o valores indicados en el cuerpo o los parámetros.", content = @Content),
        @ApiResponse(responseCode = "401", description = "Falta un JWT Bearer válido, o el token ha expirado o tiene una firma incorrecta.", content = @Content),
        @ApiResponse(responseCode = "403", description = "Solicitud rechazada por la política de acceso o por un origen CORS no permitido.", content = @Content),
        @ApiResponse(responseCode = "404", description = "No se encontró el recurso solicitado o una dependencia identificada por la operación.", content = @Content),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content)
    })
    @PostMapping("/{fuelProductId}/update-stock")
    public ResponseEntity<?> updateStock(@PathVariable Long fuelProductId,
                                         @RequestBody UpdateFuelProductStockResource resource) {
        var command = UpdateFuelProductStockCommandFromResourceAssembler.toCommandFromResource(fuelProductId, resource);
        var result = fuelProductCommandService.handle(command);
        return ResponseEntityAssembler.toResponseEntityFromResult(
                result,
                FuelProductResourceFromEntityAssembler::toResourceFromEntity,
                HttpStatus.OK);
    }

    @Operation(
        summary = "Listar productos de combustible",
        description = "Devuelve el catálogo de productos de combustible registrados, con sus precios y existencias.",
        security = { @SecurityRequirement(name = "bearerAuth") }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operación completada correctamente.", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = com.halofuel.fuelbridge.platform.inventory.interfaces.rest.resources.FuelProductResource.class)), examples = @ExampleObject(name = "respuesta", value = "[{\"id\":303,\"name\":\"Diesel B5 S-50\",\"fuelType\":\"DIESEL\",\"pricePerUnit\":4.8,\"unit\":\"L\",\"availableStock\":18000,\"capacity\":25000,\"providerId\":202,\"active\":true}]"))),
        @ApiResponse(responseCode = "401", description = "Falta un JWT Bearer válido, o el token ha expirado o tiene una firma incorrecta.", content = @Content),
        @ApiResponse(responseCode = "403", description = "Solicitud rechazada por la política de acceso o por un origen CORS no permitido.", content = @Content),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content)
    })
    @GetMapping
    public ResponseEntity<List<FuelProductResource>> getAllFuelProducts() {
        var products = fuelProductQueryService.handle(new GetAllFuelProductsQuery());
        var resources = products.stream().map(FuelProductResourceFromEntityAssembler::toResourceFromEntity).toList();
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    @Operation(
        summary = "Consultar producto de combustible",
        description = "Devuelve los datos y existencias del producto identificado.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = {
            @Parameter(name = "fuelProductId", in = ParameterIn.PATH, required = true, description = "Identificador del producto de combustible.", example = "303")
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operación completada correctamente.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.halofuel.fuelbridge.platform.inventory.interfaces.rest.resources.FuelProductResource.class), examples = @ExampleObject(name = "respuesta", value = "{\"id\":303,\"name\":\"Diesel B5 S-50\",\"fuelType\":\"DIESEL\",\"pricePerUnit\":4.8,\"unit\":\"L\",\"availableStock\":18000,\"capacity\":25000,\"providerId\":202,\"active\":true}"))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida: revisar identificadores, campos o valores indicados en el cuerpo o los parámetros.", content = @Content),
        @ApiResponse(responseCode = "401", description = "Falta un JWT Bearer válido, o el token ha expirado o tiene una firma incorrecta.", content = @Content),
        @ApiResponse(responseCode = "403", description = "Solicitud rechazada por la política de acceso o por un origen CORS no permitido.", content = @Content),
        @ApiResponse(responseCode = "404", description = "No se encontró el recurso solicitado o una dependencia identificada por la operación.", content = @Content),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content)
    })
    @GetMapping("/{fuelProductId}")
    public ResponseEntity<FuelProductResource> getFuelProductById(@PathVariable Long fuelProductId) {
        var result = fuelProductQueryService.handle(new GetFuelProductByIdQuery(fuelProductId));
        return result.map(p -> new ResponseEntity<>(
                        FuelProductResourceFromEntityAssembler.toResourceFromEntity(p), HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @Operation(
        summary = "Listar combustibles de un proveedor",
        description = "Devuelve los productos pertenecientes al proveedor; una lista vacía indica que no tiene productos registrados.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = {
            @Parameter(name = "providerId", in = ParameterIn.PATH, required = true, description = "Identificador de la empresa proveedora.", example = "202")
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operación completada correctamente.", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = com.halofuel.fuelbridge.platform.inventory.interfaces.rest.resources.FuelProductResource.class)), examples = @ExampleObject(name = "respuesta", value = "[{\"id\":303,\"name\":\"Diesel B5 S-50\",\"fuelType\":\"DIESEL\",\"pricePerUnit\":4.8,\"unit\":\"L\",\"availableStock\":18000,\"capacity\":25000,\"providerId\":202,\"active\":true}]"))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida: revisar identificadores, campos o valores indicados en el cuerpo o los parámetros.", content = @Content),
        @ApiResponse(responseCode = "401", description = "Falta un JWT Bearer válido, o el token ha expirado o tiene una firma incorrecta.", content = @Content),
        @ApiResponse(responseCode = "403", description = "Solicitud rechazada por la política de acceso o por un origen CORS no permitido.", content = @Content),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content)
    })
    @GetMapping("/provider/{providerId}")
    public ResponseEntity<List<FuelProductResource>> getFuelProductsByProvider(@PathVariable Long providerId) {
        var products = fuelProductQueryService.handle(new GetFuelProductsByProviderIdQuery(providerId));
        var resources = products.stream().map(FuelProductResourceFromEntityAssembler::toResourceFromEntity).toList();
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    @Operation(
        summary = "Actualizar producto de combustible",
        description = "Actualiza el producto identificado sin cambiar su proveedor propietario.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = {
            @Parameter(name = "fuelProductId", in = ParameterIn.PATH, required = true, description = "Identificador del producto de combustible.", example = "303")
        },
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true, description = "Datos necesarios para actualizar producto de combustible.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.halofuel.fuelbridge.platform.inventory.interfaces.rest.resources.UpdateFuelProductResource.class),
                examples = { @ExampleObject(name = "principal", value = "{\"name\":\"Diesel B5 S-50\",\"fuelType\":\"DIESEL\",\"pricePerUnit\":4.9,\"unit\":\"L\",\"availableStock\":18000,\"capacity\":25000,\"active\":true}") }))
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operación completada correctamente.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.halofuel.fuelbridge.platform.inventory.interfaces.rest.resources.FuelProductResource.class), examples = @ExampleObject(name = "respuesta", value = "{\"id\":303,\"name\":\"Diesel B5 S-50\",\"fuelType\":\"DIESEL\",\"pricePerUnit\":4.9,\"unit\":\"L\",\"availableStock\":18000,\"capacity\":25000,\"providerId\":202,\"active\":true}"))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida: revisar identificadores, campos o valores indicados en el cuerpo o los parámetros.", content = @Content),
        @ApiResponse(responseCode = "401", description = "Falta un JWT Bearer válido, o el token ha expirado o tiene una firma incorrecta.", content = @Content),
        @ApiResponse(responseCode = "403", description = "Solicitud rechazada por la política de acceso o por un origen CORS no permitido.", content = @Content),
        @ApiResponse(responseCode = "404", description = "No se encontró el recurso solicitado o una dependencia identificada por la operación.", content = @Content),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content)
    })
    @PutMapping("/{fuelProductId}")
    public ResponseEntity<?> updateFuelProduct(@PathVariable Long fuelProductId,
                                               @RequestBody UpdateFuelProductResource resource) {
        var command = UpdateFuelProductCommandFromResourceAssembler.toCommandFromResource(fuelProductId, resource);
        var result = fuelProductCommandService.handle(command);
        return ResponseEntityAssembler.toResponseEntityFromResult(
                result,
                FuelProductResourceFromEntityAssembler::toResourceFromEntity,
                HttpStatus.OK);
    }

    @Operation(
        summary = "Eliminar producto de combustible",
        description = "Elimina el producto si existe y no está referenciado por pedidos o solicitudes.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = {
            @Parameter(name = "fuelProductId", in = ParameterIn.PATH, required = true, description = "Identificador del producto de combustible.", example = "303")
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Recurso eliminado. Ejemplo HTTP: HTTP/1.1 204 No Content (sin cuerpo).", content = @Content),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida: revisar identificadores, campos o valores indicados en el cuerpo o los parámetros.", content = @Content),
        @ApiResponse(responseCode = "401", description = "Falta un JWT Bearer válido, o el token ha expirado o tiene una firma incorrecta.", content = @Content),
        @ApiResponse(responseCode = "403", description = "Solicitud rechazada por la política de acceso o por un origen CORS no permitido.", content = @Content),
        @ApiResponse(responseCode = "404", description = "No se encontró el recurso solicitado o una dependencia identificada por la operación.", content = @Content),
        @ApiResponse(responseCode = "409", description = "La operación entra en conflicto con un registro existente o con la disponibilidad del recurso.", content = @Content),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content)
    })
    @DeleteMapping("/{fuelProductId}")
    public ResponseEntity<?> deleteFuelProduct(@PathVariable Long fuelProductId) {
        var result = fuelProductCommandService.handle(new DeleteFuelProductCommand(fuelProductId));
        return ResponseEntityAssembler.toResponseEntityFromResult(
                result,
                ignored -> null,
                HttpStatus.NO_CONTENT);
    }
}
