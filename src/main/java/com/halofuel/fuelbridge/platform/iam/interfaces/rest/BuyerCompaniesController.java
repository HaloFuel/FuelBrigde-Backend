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
import com.halofuel.fuelbridge.platform.iam.interfaces.rest.resources.BuyerCompanyResource;
import com.halofuel.fuelbridge.platform.iam.interfaces.rest.resources.CreateBuyerCompanyResource;
import com.halofuel.fuelbridge.platform.iam.interfaces.rest.transform.BuyerCompanyResourceFromEntityAssembler;
import com.halofuel.fuelbridge.platform.iam.interfaces.rest.transform.CreateBuyerCompanyCommandFromResourceAssembler;
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

    public BuyerCompaniesController(BuyerCompanyCommandService buyerCompanyCommandService,
                                    BuyerCompanyQueryService buyerCompanyQueryService,
                                    BuyerCompanyRepository buyerCompanyRepository) {
        this.buyerCompanyCommandService = buyerCompanyCommandService;
        this.buyerCompanyQueryService = buyerCompanyQueryService;
        this.buyerCompanyRepository = buyerCompanyRepository;
    }

    @io.swagger.v3.oas.annotations.security.SecurityRequirements
    @Operation(
        summary = "Registrar empresa compradora",
        description = "Registra una empresa compradora con su RUC, sector y datos de contacto. Este registro es público y no requiere JWT.",
        security = {},
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true, description = "Datos necesarios para registrar empresa compradora.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.halofuel.fuelbridge.platform.iam.interfaces.rest.resources.CreateBuyerCompanyResource.class),
                examples = { @ExampleObject(name = "principal", value = "{\"name\":\"Constructora Costa Sur SAC\",\"ruc\":\"20609876543\",\"sector\":\"CONSTRUCTION\",\"address\":\"Av. Separadora Industrial 2450, Lima\",\"contactEmail\":\"compras@costasur.example\",\"phone\":\"+51987654321\"}") }))
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Registro creado correctamente.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.halofuel.fuelbridge.platform.iam.interfaces.rest.resources.BuyerCompanyResource.class), examples = @ExampleObject(name = "respuesta", value = "{\"id\":101,\"name\":\"Constructora Costa Sur SAC\",\"ruc\":\"20609876543\",\"sector\":\"CONSTRUCTION\",\"address\":\"Av. Separadora Industrial 2450, Lima\",\"contactEmail\":\"compras@costasur.example\",\"phone\":\"+51987654321\"}"))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida: revisar identificadores, campos o valores indicados en el cuerpo o los parámetros.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "error", summary = "Solicitud inválida para registrar empresa compradora", description = "Ejemplo de error con la estructura ErrorResource (code, message, details).", value = "{\"code\":\"VALIDATION_ERROR\",\"message\":\"Solicitud inválida para registrar empresa compradora\",\"details\":\"La empresa Constructora Costa Sur SAC requiere un RUC de 11 dígitos y un correo de contacto válido.\"}"))),
        @ApiResponse(responseCode = "403", description = "Origen CORS rechazado: devuelve el texto literal Invalid CORS request sin cabecera Content-Type. Un rechazo de autorización de Spring Security puede no incluir cuerpo.", content = @Content(mediaType = "*/*", schema = @Schema(type = "string"), examples = @ExampleObject(name = "corsRechazado", description = "Texto literal devuelto por el procesador CORS; la respuesta no incluye Content-Type.", value = "Invalid CORS request"))),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "error", summary = "No se pudo completar la operación: registrar empresa compradora", description = "Ejemplo de error con la estructura ErrorResource (code, message, details).", value = "{\"code\":\"UNEXPECTED_ERROR\",\"message\":\"No se pudo completar la operación: registrar empresa compradora\",\"details\":\"Ocurrió un error al procesar los datos de empresa compradora en FuelBridge.\"}")))
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
        @ApiResponse(responseCode = "200", description = "Operación completada correctamente.", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = com.halofuel.fuelbridge.platform.iam.interfaces.rest.resources.BuyerCompanyResource.class)), examples = @ExampleObject(name = "respuesta", value = "[{\"id\":101,\"name\":\"Constructora Costa Sur SAC\",\"ruc\":\"20609876543\",\"sector\":\"CONSTRUCTION\",\"address\":\"Av. Separadora Industrial 2450, Lima\",\"contactEmail\":\"compras@costasur.example\",\"phone\":\"+51987654321\"}]"))),
        @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado: 401 sin cuerpo de respuesta; el despacho a /error vuelve a pasar por la seguridad actual.", content = @Content),
        @ApiResponse(responseCode = "403", description = "Origen CORS rechazado: devuelve el texto literal Invalid CORS request sin cabecera Content-Type. Un rechazo de autorización de Spring Security puede no incluir cuerpo.", content = @Content(mediaType = "*/*", schema = @Schema(type = "string"), examples = @ExampleObject(name = "corsRechazado", description = "Texto literal devuelto por el procesador CORS; la respuesta no incluye Content-Type.", value = "Invalid CORS request"))),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "error", summary = "No se pudo completar la operación: listar empresas compradoras", description = "Ejemplo de error con la estructura ErrorResource (code, message, details).", value = "{\"code\":\"UNEXPECTED_ERROR\",\"message\":\"No se pudo completar la operación: listar empresas compradoras\",\"details\":\"Ocurrió un error al procesar los datos de empresa compradora en FuelBridge.\"}")))
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
        @ApiResponse(responseCode = "200", description = "Operación completada correctamente.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.halofuel.fuelbridge.platform.iam.interfaces.rest.resources.BuyerCompanyResource.class), examples = @ExampleObject(name = "respuesta", value = "{\"id\":101,\"name\":\"Constructora Costa Sur SAC\",\"ruc\":\"20609876543\",\"sector\":\"CONSTRUCTION\",\"address\":\"Av. Separadora Industrial 2450, Lima\",\"contactEmail\":\"compras@costasur.example\",\"phone\":\"+51987654321\"}"))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida: revisar identificadores, campos o valores indicados en el cuerpo o los parámetros.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "error", summary = "Solicitud inválida para consultar empresa compradora", description = "Ejemplo de error con la estructura ErrorResource (code, message, details).", value = "{\"code\":\"VALIDATION_ERROR\",\"message\":\"Solicitud inválida para consultar empresa compradora\",\"details\":\"El parámetro companyId=abc no es un identificador numérico válido de empresa compradora.\"}"))),
        @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado: 401 sin cuerpo de respuesta; el despacho a /error vuelve a pasar por la seguridad actual.", content = @Content),
        @ApiResponse(responseCode = "403", description = "Origen CORS rechazado: devuelve el texto literal Invalid CORS request sin cabecera Content-Type. Un rechazo de autorización de Spring Security puede no incluir cuerpo.", content = @Content(mediaType = "*/*", schema = @Schema(type = "string"), examples = @ExampleObject(name = "corsRechazado", description = "Texto literal devuelto por el procesador CORS; la respuesta no incluye Content-Type.", value = "Invalid CORS request"))),
        @ApiResponse(responseCode = "404", description = "Recurso no encontrado: sin cuerpo de respuesta.", content = @Content),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "error", summary = "No se pudo completar la operación: consultar empresa compradora", description = "Ejemplo de error con la estructura ErrorResource (code, message, details).", value = "{\"code\":\"UNEXPECTED_ERROR\",\"message\":\"No se pudo completar la operación: consultar empresa compradora\",\"details\":\"Ocurrió un error al procesar los datos de empresa compradora en FuelBridge.\"}")))
    })
    @GetMapping("/{companyId}")
    public ResponseEntity<BuyerCompanyResource> getBuyerCompanyById(@PathVariable Long companyId) {
        var result = buyerCompanyQueryService.handle(new GetBuyerCompanyByIdQuery(companyId));
        return result.map(company -> new ResponseEntity<>(
                        BuyerCompanyResourceFromEntityAssembler.toResourceFromEntity(company), HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
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
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.halofuel.fuelbridge.platform.iam.interfaces.rest.resources.CreateBuyerCompanyResource.class),
                examples = { @ExampleObject(name = "principal", value = "{\"name\":\"Constructora Costa Sur SAC\",\"ruc\":\"20609876543\",\"sector\":\"CONSTRUCTION\",\"address\":\"Av. Separadora Industrial 2450, Lima\",\"contactEmail\":\"compras@costasur.example\",\"phone\":\"+51999888777\"}") }))
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operación completada correctamente.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.halofuel.fuelbridge.platform.iam.interfaces.rest.resources.BuyerCompanyResource.class), examples = @ExampleObject(name = "respuesta", value = "{\"id\":101,\"name\":\"Constructora Costa Sur SAC\",\"ruc\":\"20609876543\",\"sector\":\"CONSTRUCTION\",\"address\":\"Av. Separadora Industrial 2450, Lima\",\"contactEmail\":\"compras@costasur.example\",\"phone\":\"+51999888777\"}"))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida: revisar identificadores, campos o valores indicados en el cuerpo o los parámetros.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "error", summary = "Solicitud inválida para actualizar empresa compradora", description = "Ejemplo de error con la estructura ErrorResource (code, message, details).", value = "{\"code\":\"VALIDATION_ERROR\",\"message\":\"Solicitud inválida para actualizar empresa compradora\",\"details\":\"La empresa Constructora Costa Sur SAC requiere un RUC de 11 dígitos y un correo de contacto válido.\"}"))),
        @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado: 401 sin cuerpo de respuesta; el despacho a /error vuelve a pasar por la seguridad actual.", content = @Content),
        @ApiResponse(responseCode = "403", description = "Origen CORS rechazado: devuelve el texto literal Invalid CORS request sin cabecera Content-Type. Un rechazo de autorización de Spring Security puede no incluir cuerpo.", content = @Content(mediaType = "*/*", schema = @Schema(type = "string"), examples = @ExampleObject(name = "corsRechazado", description = "Texto literal devuelto por el procesador CORS; la respuesta no incluye Content-Type.", value = "Invalid CORS request"))),
        @ApiResponse(responseCode = "404", description = "Recurso no encontrado: sin cuerpo de respuesta.", content = @Content),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "error", summary = "No se pudo completar la operación: actualizar empresa compradora", description = "Ejemplo de error con la estructura ErrorResource (code, message, details).", value = "{\"code\":\"UNEXPECTED_ERROR\",\"message\":\"No se pudo completar la operación: actualizar empresa compradora\",\"details\":\"Ocurrió un error al procesar los datos de empresa compradora en FuelBridge.\"}")))
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
