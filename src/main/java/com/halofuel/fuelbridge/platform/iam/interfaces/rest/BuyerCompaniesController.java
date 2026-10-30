package com.halofuel.fuelbridge.platform.iam.interfaces.rest;

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

import com.halofuel.fuelbridge.platform.iam.application.commandservices.BuyerCompanyCommandService;
import com.halofuel.fuelbridge.platform.iam.application.queryservices.BuyerCompanyQueryService;
import com.halofuel.fuelbridge.platform.iam.domain.model.queries.GetAllBuyerCompaniesQuery;
import com.halofuel.fuelbridge.platform.iam.domain.model.queries.GetBuyerCompanyByIdQuery;
import com.halofuel.fuelbridge.platform.iam.domain.repositories.BuyerCompanyRepository;
import com.halofuel.fuelbridge.platform.iam.interfaces.rest.resources.BuyerCompanyDetailResource;
import com.halofuel.fuelbridge.platform.iam.interfaces.rest.resources.BuyerCompanyResource;
import com.halofuel.fuelbridge.platform.iam.interfaces.rest.resources.CreateBuyerCompanyResource;
import com.halofuel.fuelbridge.platform.iam.interfaces.rest.transform.BuyerCompanyDetailResourceAssembler;
import com.halofuel.fuelbridge.platform.iam.interfaces.rest.transform.BuyerCompanyResourceFromEntityAssembler;
import com.halofuel.fuelbridge.platform.iam.interfaces.rest.transform.CreateBuyerCompanyCommandFromResourceAssembler;
import com.halofuel.fuelbridge.platform.ordering.application.queryservices.FuelOrderQueryService;
import com.halofuel.fuelbridge.platform.ordering.domain.model.queries.GetFuelOrdersByCompanyIdQuery;
import com.halofuel.fuelbridge.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * BuyerCompaniesController
 *
 * REST controller that exposes CRUD endpoints for buyer company management (US-40).
 * Handles registration, listing, retrieval and update of companies
 * that request fuel through the FuelBridge platform.
 * Includes order history retrieval per company (US-29/TS-09).
 *
 * Base path: /api/v1/buyer-companies
 */
@RestController
@RequestMapping(value = "/api/v1/buyer-companies", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Buyer Companies", description = "Buyer company management endpoints")
public class BuyerCompaniesController {

    private final BuyerCompanyCommandService buyerCompanyCommandService;
    private final BuyerCompanyQueryService buyerCompanyQueryService;
    private final BuyerCompanyRepository buyerCompanyRepository;
    private final FuelOrderQueryService fuelOrderQueryService;

    public BuyerCompaniesController(BuyerCompanyCommandService buyerCompanyCommandService,
                                    BuyerCompanyQueryService buyerCompanyQueryService,
                                    BuyerCompanyRepository buyerCompanyRepository,
                                    FuelOrderQueryService fuelOrderQueryService) {
        this.buyerCompanyCommandService = buyerCompanyCommandService;
        this.buyerCompanyQueryService = buyerCompanyQueryService;
        this.buyerCompanyRepository = buyerCompanyRepository;
        this.fuelOrderQueryService = fuelOrderQueryService;
    }

    @io.swagger.v3.oas.annotations.security.SecurityRequirements
    @Operation(
            summary = "Registrar empresa compradora",
            description = "Registra una empresa compradora con su RUC, sector y datos de contacto. Este registro es público y no requiere JWT.",
            security = {},
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true, description = "Datos necesarios para registrar empresa compradora.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CreateBuyerCompanyResource.class),
                            examples = { @ExampleObject(name = "principal", value = "{\"name\":\"Constructora Costa Sur SAC\",\"ruc\":\"20609876543\",\"sector\":\"CONSTRUCTION\",\"address\":\"Av. Separadora Industrial 2450, Lima\",\"contactEmail\":\"compras@costasur.example\",\"phone\":\"+51987654321\"}") }))
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Registro creado correctamente.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = BuyerCompanyResource.class), examples = @ExampleObject(name = "respuesta", value = "{\"id\":101,\"name\":\"Constructora Costa Sur SAC\",\"ruc\":\"20609876543\",\"sector\":\"CONSTRUCTION\",\"address\":\"Av. Separadora Industrial 2450, Lima\",\"contactEmail\":\"compras@costasur.example\",\"phone\":\"+51987654321\"}"))),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida: revisar identificadores, campos o valores indicados en el cuerpo o los parámetros.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "error", value = "{\"code\":\"VALIDATION_ERROR\",\"message\":\"Solicitud inválida para registrar empresa compradora\",\"details\":\"La empresa Constructora Costa Sur SAC requiere un RUC de 11 dígitos y un correo de contacto válido.\"}"))),
            @ApiResponse(responseCode = "403", description = "Origen CORS rechazado.", content = @Content(mediaType = "*/*", schema = @Schema(type = "string"), examples = @ExampleObject(name = "corsRechazado", value = "Invalid CORS request"))),
            @ApiResponse(responseCode = "500", description = "Error interno.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "error", value = "{\"code\":\"UNEXPECTED_ERROR\",\"message\":\"No se pudo completar la operación: registrar empresa compradora\",\"details\":\"Ocurrió un error al procesar los datos de empresa compradora en FuelBridge.\"}")))
    })
    @PostMapping
    public ResponseEntity<?> createBuyerCompany(@RequestBody CreateBuyerCompanyResource resource) {
        var command = CreateBuyerCompanyCommandFromResourceAssembler.toCommandFromResource(resource);
        var result = buyerCompanyCommandService.handle(command);
        return ResponseEntityAssembler.toResponseEntityFromResult(
                result,
                BuyerCompanyResourceFromEntityAssembler::toResourceFromEntity,
                HttpStatus.CREATED);
    }

    @Operation(
            summary = "Listar empresas compradoras",
            description = "Devuelve las empresas compradoras registradas y sus datos de contacto.",
            security = { @SecurityRequirement(name = "bearerAuth") }
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Operación completada correctamente.", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = BuyerCompanyResource.class)), examples = @ExampleObject(name = "respuesta", value = "[{\"id\":101,\"name\":\"Constructora Costa Sur SAC\",\"ruc\":\"20609876543\",\"sector\":\"CONSTRUCTION\",\"address\":\"Av. Separadora Industrial 2450, Lima\",\"contactEmail\":\"compras@costasur.example\",\"phone\":\"+51987654321\"}]"))),
            @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado.", content = @Content),
            @ApiResponse(responseCode = "403", description = "Origen CORS rechazado.", content = @Content(mediaType = "*/*", schema = @Schema(type = "string"), examples = @ExampleObject(name = "corsRechazado", value = "Invalid CORS request"))),
            @ApiResponse(responseCode = "500", description = "Error interno.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "error", value = "{\"code\":\"UNEXPECTED_ERROR\",\"message\":\"No se pudo completar la operación: listar empresas compradoras\",\"details\":\"Ocurrió un error al procesar los datos de empresa compradora en FuelBridge.\"}")))
    })
    @GetMapping
    public ResponseEntity<List<BuyerCompanyResource>> getAllBuyerCompanies() {
        var companies = buyerCompanyQueryService.handle(new GetAllBuyerCompaniesQuery());
        var resources = companies.stream().map(BuyerCompanyResourceFromEntityAssembler::toResourceFromEntity).toList();
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    @Operation(
            summary = "Consultar empresa compradora",
            description = "Devuelve la empresa compradora identificada.",
            security = { @SecurityRequirement(name = "bearerAuth") },
            parameters = {
                    @Parameter(name = "companyId", in = ParameterIn.PATH, required = true, description = "Identificador de la empresa compradora.", example = "101")
            }
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Operación completada correctamente.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = BuyerCompanyResource.class), examples = @ExampleObject(name = "respuesta", value = "{\"id\":101,\"name\":\"Constructora Costa Sur SAC\",\"ruc\":\"20609876543\",\"sector\":\"CONSTRUCTION\",\"address\":\"Av. Separadora Industrial 2450, Lima\",\"contactEmail\":\"compras@costasur.example\",\"phone\":\"+51987654321\"}"))),
            @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado.", content = @Content),
            @ApiResponse(responseCode = "404", description = "Recurso no encontrado.", content = @Content),
            @ApiResponse(responseCode = "500", description = "Error interno.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "error", value = "{\"code\":\"UNEXPECTED_ERROR\",\"message\":\"No se pudo completar la operación: consultar empresa compradora\",\"details\":\"Ocurrió un error al procesar los datos de empresa compradora en FuelBridge.\"}")))
    })
    @GetMapping("/{companyId}")
    public ResponseEntity<BuyerCompanyResource> getBuyerCompanyById(@PathVariable Long companyId) {
        var result = buyerCompanyQueryService.handle(new GetBuyerCompanyByIdQuery(companyId));
        return result.map(company -> new ResponseEntity<>(
                        BuyerCompanyResourceFromEntityAssembler.toResourceFromEntity(company), HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @Operation(
            summary = "Consultar detalle de empresa compradora con historial de pedidos",
            description = "Devuelve los datos de la empresa compradora junto con su historial completo de pedidos de combustible.",
            security = { @SecurityRequirement(name = "bearerAuth") },
            parameters = {
                    @Parameter(name = "companyId", in = ParameterIn.PATH, required = true, description = "Identificador de la empresa compradora.", example = "101")
            }
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Operación completada correctamente.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = BuyerCompanyDetailResource.class), examples = @ExampleObject(name = "respuesta", value = "{\"id\":101,\"name\":\"Constructora Costa Sur SAC\",\"ruc\":\"20609876543\",\"sector\":\"CONSTRUCTION\",\"address\":\"Av. Separadora Industrial 2450, Lima\",\"contactEmail\":\"compras@costasur.example\",\"phone\":\"+51987654321\",\"orders\":[{\"id\":1,\"requestedQuantity\":500.0,\"totalPrice\":1500.0,\"status\":\"CONFIRMED\",\"deliveryAddress\":\"Av. Industrial 123\",\"scheduledDate\":\"2026-10-15\"}]}"))),
            @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado.", content = @Content),
            @ApiResponse(responseCode = "404", description = "Empresa compradora no encontrada.", content = @Content),
            @ApiResponse(responseCode = "500", description = "Error interno.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "error", value = "{\"code\":\"UNEXPECTED_ERROR\",\"message\":\"No se pudo completar la operación: consultar detalle de empresa compradora\",\"details\":\"Ocurrió un error al procesar los datos de empresa compradora en FuelBridge.\"}")))
    })
    @GetMapping("/{companyId}/details")
    public ResponseEntity<BuyerCompanyDetailResource> getBuyerCompanyDetail(@PathVariable Long companyId) {
        var result = buyerCompanyQueryService.handle(new GetBuyerCompanyByIdQuery(companyId));
        if (result.isEmpty()) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        var orders = fuelOrderQueryService.handle(new GetFuelOrdersByCompanyIdQuery(companyId));
        return new ResponseEntity<>(
                BuyerCompanyDetailResourceAssembler.toResource(result.get(), orders),
                HttpStatus.OK);
    }

    @Operation(
            summary = "Actualizar empresa compradora",
            description = "Reemplaza nombre, RUC, sector, dirección, correo y teléfono de la empresa existente.",
            security = { @SecurityRequirement(name = "bearerAuth") },
            parameters = {
                    @Parameter(name = "companyId", in = ParameterIn.PATH, required = true, description = "Identificador de la empresa compradora.", example = "101")
            },
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true, description = "Datos necesarios para actualizar empresa compradora.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CreateBuyerCompanyResource.class),
                            examples = { @ExampleObject(name = "principal", value = "{\"name\":\"Constructora Costa Sur SAC\",\"ruc\":\"20609876543\",\"sector\":\"CONSTRUCTION\",\"address\":\"Av. Separadora Industrial 2450, Lima\",\"contactEmail\":\"compras@costasur.example\",\"phone\":\"+51999888777\"}") }))
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Operación completada correctamente.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = BuyerCompanyResource.class), examples = @ExampleObject(name = "respuesta", value = "{\"id\":101,\"name\":\"Constructora Costa Sur SAC\",\"ruc\":\"20609876543\",\"sector\":\"CONSTRUCTION\",\"address\":\"Av. Separadora Industrial 2450, Lima\",\"contactEmail\":\"compras@costasur.example\",\"phone\":\"+51999888777\"}"))),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "error", value = "{\"code\":\"VALIDATION_ERROR\",\"message\":\"Solicitud inválida para actualizar empresa compradora\",\"details\":\"La empresa Constructora Costa Sur SAC requiere un RUC de 11 dígitos y un correo de contacto válido.\"}"))),
            @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado.", content = @Content),
            @ApiResponse(responseCode = "404", description = "Recurso no encontrado.", content = @Content),
            @ApiResponse(responseCode = "500", description = "Error interno.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "error", value = "{\"code\":\"UNEXPECTED_ERROR\",\"message\":\"No se pudo completar la operación: actualizar empresa compradora\",\"details\":\"Ocurrió un error al procesar los datos de empresa compradora en FuelBridge.\"}")))
    })
    @PutMapping("/{companyId}")
    public ResponseEntity<BuyerCompanyResource> updateBuyerCompany(@PathVariable Long companyId,
                                                                   @RequestBody CreateBuyerCompanyResource resource) {
        var result = buyerCompanyRepository.findById(companyId);
        if (result.isEmpty()) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        var company = result.get();
        company.setName(resource.name());
        company.setRuc(resource.ruc());
        company.setSector(resource.sector());
        company.setAddress(resource.address());
        company.setContactEmail(resource.contactEmail());
        company.setPhone(resource.phone());
        var updated = buyerCompanyRepository.save(company);
        return new ResponseEntity<>(BuyerCompanyResourceFromEntityAssembler.toResourceFromEntity(updated), HttpStatus.OK);
    }
}