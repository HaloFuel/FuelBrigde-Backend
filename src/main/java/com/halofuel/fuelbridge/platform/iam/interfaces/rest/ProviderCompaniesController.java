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

import com.halofuel.fuelbridge.platform.iam.application.commandservices.ProviderCompanyCommandService;
import com.halofuel.fuelbridge.platform.iam.application.queryservices.ProviderCompanyQueryService;
import com.halofuel.fuelbridge.platform.iam.domain.model.queries.GetAllProviderCompaniesQuery;
import com.halofuel.fuelbridge.platform.iam.domain.model.queries.GetProviderCompanyByIdQuery;
import com.halofuel.fuelbridge.platform.iam.domain.repositories.ProviderCompanyRepository;
import com.halofuel.fuelbridge.platform.iam.interfaces.rest.resources.CreateProviderCompanyResource;
import com.halofuel.fuelbridge.platform.iam.interfaces.rest.resources.ProviderCompanyResource;
import com.halofuel.fuelbridge.platform.iam.interfaces.rest.transform.CreateProviderCompanyCommandFromResourceAssembler;
import com.halofuel.fuelbridge.platform.iam.interfaces.rest.transform.ProviderCompanyResourceFromEntityAssembler;
import com.halofuel.fuelbridge.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ProviderCompaniesController
 *
 * REST controller that exposes CRUD endpoints for provider company management (US-41).
 * Handles registration, listing, retrieval and update of fuel supplier companies
 * operating within the FuelBridge platform.
 *
 * Base path: /api/v1/provider-companies
 */

@RestController
@RequestMapping(value = "/api/v1/provider-companies", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Provider Companies", description = "Provider company management endpoints")
public class ProviderCompaniesController {

    private final ProviderCompanyCommandService providerCompanyCommandService;
    private final ProviderCompanyQueryService providerCompanyQueryService;
    private final ProviderCompanyRepository providerCompanyRepository;

    public ProviderCompaniesController(ProviderCompanyCommandService providerCompanyCommandService,
                                       ProviderCompanyQueryService providerCompanyQueryService,
                                       ProviderCompanyRepository providerCompanyRepository) {
        this.providerCompanyCommandService = providerCompanyCommandService;
        this.providerCompanyQueryService = providerCompanyQueryService;
        this.providerCompanyRepository = providerCompanyRepository;
    }

    @io.swagger.v3.oas.annotations.security.SecurityRequirements
    @Operation(
        summary = "Registrar empresa proveedora",
        description = "Registra un proveedor con su RUC, dirección, combustibles ofrecidos y descripción. Este registro es público y no requiere JWT.",
        security = {},
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true, description = "Datos necesarios para registrar empresa proveedora.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.halofuel.fuelbridge.platform.iam.interfaces.rest.resources.CreateProviderCompanyResource.class),
                examples = { @ExampleObject(name = "principal", value = "{\"name\":\"Combustibles Andina SAC\",\"ruc\":\"20601234567\",\"rating\":4.7,\"address\":\"Av. Nestor Gambetta 5800, Callao\",\"phone\":\"+51976543210\",\"fuelTypesOffered\":[\"DIESEL\",\"GASOLINE_95\"],\"description\":\"Suministro de combustibles para obras y flotas en Lima y Callao.\"}") }))
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Registro creado correctamente.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.halofuel.fuelbridge.platform.iam.interfaces.rest.resources.ProviderCompanyResource.class), examples = @ExampleObject(name = "respuesta", value = "{\"id\":202,\"name\":\"Combustibles Andina SAC\",\"ruc\":\"20601234567\",\"rating\":4.7,\"address\":\"Av. Nestor Gambetta 5800, Callao\",\"phone\":\"+51976543210\",\"fuelTypesOffered\":[\"DIESEL\",\"GASOLINE_95\"],\"description\":\"Suministro de combustibles para obras y flotas en Lima y Callao.\"}"))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida: revisar identificadores, campos o valores indicados en el cuerpo o los parámetros.", content = @Content),
        @ApiResponse(responseCode = "403", description = "Solicitud rechazada por la política de acceso o por un origen CORS no permitido.", content = @Content),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content)
    })
    @PostMapping
    public ResponseEntity<?> createProviderCompany(@RequestBody CreateProviderCompanyResource resource) {
        var command = CreateProviderCompanyCommandFromResourceAssembler.toCommandFromResource(resource);
        var result = providerCompanyCommandService.handle(command);
        return ResponseEntityAssembler.toResponseEntityFromResult(
                result,
                ProviderCompanyResourceFromEntityAssembler::toResourceFromEntity,
                HttpStatus.CREATED);
    }

    @Operation(
        summary = "Listar empresas proveedoras",
        description = "Devuelve los proveedores registrados con los combustibles que ofrecen y sus datos empresariales.",
        security = { @SecurityRequirement(name = "bearerAuth") }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operación completada correctamente.", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = com.halofuel.fuelbridge.platform.iam.interfaces.rest.resources.ProviderCompanyResource.class)), examples = @ExampleObject(name = "respuesta", value = "[{\"id\":202,\"name\":\"Combustibles Andina SAC\",\"ruc\":\"20601234567\",\"rating\":4.7,\"address\":\"Av. Nestor Gambetta 5800, Callao\",\"phone\":\"+51976543210\",\"fuelTypesOffered\":[\"DIESEL\",\"GASOLINE_95\"],\"description\":\"Suministro de combustibles para obras y flotas en Lima y Callao.\"}]"))),
        @ApiResponse(responseCode = "401", description = "Falta un JWT Bearer válido, o el token ha expirado o tiene una firma incorrecta.", content = @Content),
        @ApiResponse(responseCode = "403", description = "Solicitud rechazada por la política de acceso o por un origen CORS no permitido.", content = @Content),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content)
    })
    @GetMapping
    public ResponseEntity<List<ProviderCompanyResource>> getAllProviderCompanies() {
        var companies = providerCompanyQueryService.handle(new GetAllProviderCompaniesQuery());
        var resources = companies.stream().map(ProviderCompanyResourceFromEntityAssembler::toResourceFromEntity).toList();
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    @Operation(
        summary = "Consultar empresa proveedora",
        description = "Devuelve el proveedor identificado.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = {
            @Parameter(name = "providerId", in = ParameterIn.PATH, required = true, description = "Identificador de la empresa proveedora.", example = "202")
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operación completada correctamente.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.halofuel.fuelbridge.platform.iam.interfaces.rest.resources.ProviderCompanyResource.class), examples = @ExampleObject(name = "respuesta", value = "{\"id\":202,\"name\":\"Combustibles Andina SAC\",\"ruc\":\"20601234567\",\"rating\":4.7,\"address\":\"Av. Nestor Gambetta 5800, Callao\",\"phone\":\"+51976543210\",\"fuelTypesOffered\":[\"DIESEL\",\"GASOLINE_95\"],\"description\":\"Suministro de combustibles para obras y flotas en Lima y Callao.\"}"))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida: revisar identificadores, campos o valores indicados en el cuerpo o los parámetros.", content = @Content),
        @ApiResponse(responseCode = "401", description = "Falta un JWT Bearer válido, o el token ha expirado o tiene una firma incorrecta.", content = @Content),
        @ApiResponse(responseCode = "403", description = "Solicitud rechazada por la política de acceso o por un origen CORS no permitido.", content = @Content),
        @ApiResponse(responseCode = "404", description = "No se encontró el recurso solicitado o una dependencia identificada por la operación.", content = @Content),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content)
    })
    @GetMapping("/{providerId}")
    public ResponseEntity<ProviderCompanyResource> getProviderCompanyById(@PathVariable Long providerId) {
        var result = providerCompanyQueryService.handle(new GetProviderCompanyByIdQuery(providerId));
        return result.map(company -> new ResponseEntity<>(
                        ProviderCompanyResourceFromEntityAssembler.toResourceFromEntity(company), HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @Operation(
        summary = "Actualizar empresa proveedora",
        description = "Reemplaza los datos del proveedor existente, incluidos rating, fuelTypesOffered y descripción.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = {
            @Parameter(name = "providerId", in = ParameterIn.PATH, required = true, description = "Identificador de la empresa proveedora.", example = "202")
        },
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true, description = "Datos necesarios para actualizar empresa proveedora.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.halofuel.fuelbridge.platform.iam.interfaces.rest.resources.CreateProviderCompanyResource.class),
                examples = { @ExampleObject(name = "principal", value = "{\"name\":\"Combustibles Andina SAC\",\"ruc\":\"20601234567\",\"rating\":4.7,\"address\":\"Av. Nestor Gambetta 5800, Callao\",\"phone\":\"+51976543210\",\"fuelTypesOffered\":[\"DIESEL\",\"GASOLINE_95\"],\"description\":\"Suministro de combustibles para obras y flotas en Lima y Callao.\"}") }))
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operación completada correctamente.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.halofuel.fuelbridge.platform.iam.interfaces.rest.resources.ProviderCompanyResource.class), examples = @ExampleObject(name = "respuesta", value = "{\"id\":202,\"name\":\"Combustibles Andina SAC\",\"ruc\":\"20601234567\",\"rating\":4.7,\"address\":\"Av. Nestor Gambetta 5800, Callao\",\"phone\":\"+51976543210\",\"fuelTypesOffered\":[\"DIESEL\",\"GASOLINE_95\"],\"description\":\"Suministro de combustibles para obras y flotas en Lima y Callao.\"}"))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida: revisar identificadores, campos o valores indicados en el cuerpo o los parámetros.", content = @Content),
        @ApiResponse(responseCode = "401", description = "Falta un JWT Bearer válido, o el token ha expirado o tiene una firma incorrecta.", content = @Content),
        @ApiResponse(responseCode = "403", description = "Solicitud rechazada por la política de acceso o por un origen CORS no permitido.", content = @Content),
        @ApiResponse(responseCode = "404", description = "No se encontró el recurso solicitado o una dependencia identificada por la operación.", content = @Content),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content)
    })
    @PutMapping("/{providerId}")
    public ResponseEntity<ProviderCompanyResource> updateProviderCompany(@PathVariable Long providerId,
                                                                         @RequestBody CreateProviderCompanyResource resource) {
        var result = providerCompanyRepository.findById(providerId);
        if (result.isEmpty()) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        var provider = result.get();
        provider.setName(resource.name());
        provider.setRuc(resource.ruc());
        provider.setRating(resource.rating());
        provider.setAddress(resource.address());
        provider.setPhone(resource.phone());
        provider.setFuelTypesOffered(resource.fuelTypesOffered());
        provider.setDescription(resource.description());
        var updated = providerCompanyRepository.save(provider);
        return new ResponseEntity<>(ProviderCompanyResourceFromEntityAssembler.toResourceFromEntity(updated), HttpStatus.OK);
    }
}
