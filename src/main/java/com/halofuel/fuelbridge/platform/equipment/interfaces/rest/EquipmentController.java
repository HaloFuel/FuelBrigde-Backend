package com.halofuel.fuelbridge.platform.equipment.interfaces.rest;

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

import com.halofuel.fuelbridge.platform.equipment.application.commandservices.EquipmentCommandService;
import com.halofuel.fuelbridge.platform.equipment.application.queryservices.EquipmentQueryService;
import com.halofuel.fuelbridge.platform.equipment.domain.model.queries.GetAllEquipmentQuery;
import com.halofuel.fuelbridge.platform.equipment.domain.model.queries.GetEquipmentByCompanyIdQuery;
import com.halofuel.fuelbridge.platform.equipment.domain.model.queries.GetEquipmentByIdQuery;
import com.halofuel.fuelbridge.platform.equipment.interfaces.rest.resources.CreateEquipmentResource;
import com.halofuel.fuelbridge.platform.equipment.interfaces.rest.resources.EquipmentResource;
import com.halofuel.fuelbridge.platform.equipment.interfaces.rest.resources.FavoriteProviderResource;
import com.halofuel.fuelbridge.platform.equipment.interfaces.rest.resources.UpdateEquipmentResource;
import com.halofuel.fuelbridge.platform.equipment.interfaces.rest.transform.CreateEquipmentCommandFromResourceAssembler;
import com.halofuel.fuelbridge.platform.equipment.interfaces.rest.transform.EquipmentResourceFromEntityAssembler;
import com.halofuel.fuelbridge.platform.equipment.interfaces.rest.transform.UpdateEquipmentCommandFromResourceAssembler;
import com.halofuel.fuelbridge.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;
import com.halofuel.fuelbridge.platform.equipment.domain.repositories.EquipmentRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/equipment", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Equipment", description = "Equipment management endpoints")
public class EquipmentController {

    private final EquipmentCommandService equipmentCommandService;
    private final EquipmentQueryService equipmentQueryService;
    private final EquipmentRepository equipmentRepository;

    public EquipmentController(EquipmentCommandService equipmentCommandService,
                               EquipmentQueryService equipmentQueryService,
                               EquipmentRepository equipmentRepository) {
        this.equipmentCommandService = equipmentCommandService;
        this.equipmentQueryService = equipmentQueryService;
        this.equipmentRepository = equipmentRepository;
    }

    @Operation(
        summary = "Asignar proveedor favorito",
        description = "Asocia un proveedor favorito al equipo identificado y devuelve el equipo actualizado.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = {
            @Parameter(name = "equipmentId", in = ParameterIn.PATH, required = true, description = "Identificador del equipo.", example = "305")
        },
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true, description = "Datos necesarios para asignar proveedor favorito.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.halofuel.fuelbridge.platform.equipment.interfaces.rest.resources.FavoriteProviderResource.class),
                examples = { @ExampleObject(name = "principal", value = "{\"providerId\":202}") }))
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operación completada correctamente.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.halofuel.fuelbridge.platform.equipment.interfaces.rest.resources.EquipmentResource.class), examples = @ExampleObject(name = "respuesta", value = "{\"id\":305,\"name\":\"Excavadora Caterpillar 320\",\"equipmentType\":\"EXCAVATOR\",\"licensePlate\":\"EQ-320-LIM\",\"fuelType\":\"DIESEL\",\"tankCapacity\":400,\"currentLevel\":80,\"location\":\"Obra Villa El Salvador, Lima\",\"status\":\"ACTIVE\",\"autoRefill\":true,\"refillThreshold\":25,\"lastRefillDate\":\"2026-10-08\",\"companyId\":101,\"favoriteProviderId\":202}"))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida: revisar identificadores, campos o valores indicados en el cuerpo o los parámetros.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "error", summary = "Solicitud inválida para asignar proveedor favorito", description = "Ejemplo de error con la estructura ErrorResource (code, message, details).", value = "{\"code\":\"VALIDATION_ERROR\",\"message\":\"Solicitud inválida para asignar proveedor favorito\",\"details\":\"El equipo Excavadora CAT 320 debe incluir los datos obligatorios y pertenecer a la empresa compradora.\"}"))),
        @ApiResponse(responseCode = "401", description = "Falta un JWT Bearer válido, o el token ha expirado o tiene una firma incorrecta.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "Se requiere autenticación para acceder a FuelBridge", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El manejador de seguridad actual usa sendError; el cuerpo lo determina Spring.", value = "{\"code\":\"UNAUTHORIZED\",\"message\":\"Se requiere autenticación para acceder a FuelBridge\",\"details\":\"Envíe un JWT vigente emitido por FuelBridge en Authorization: Bearer <token> para asignar proveedor favorito.\"}"))),
        @ApiResponse(responseCode = "403", description = "Solicitud rechazada por la política de acceso o por un origen CORS no permitido.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "Acceso a FuelBridge no permitido", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El rechazo actual lo gestiona Spring Security o CORS y puede no incluir cuerpo.", value = "{\"code\":\"FORBIDDEN\",\"message\":\"Acceso a FuelBridge no permitido\",\"details\":\"La política de acceso o el origen CORS no permite asignar proveedor favorito.\"}"))),
        @ApiResponse(responseCode = "404", description = "No se encontró el recurso solicitado o una dependencia identificada por la operación.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "Equipo no encontrado", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). Esta ruta actualmente devuelve 404 sin cuerpo.", value = "{\"code\":\"EQUIPMENT_NOT_FOUND\",\"message\":\"Equipo no encontrado\",\"details\":\"No existe equipo con identificador 305.\"}"))),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "error", summary = "No se pudo completar la operación: asignar proveedor favorito", description = "Ejemplo de error con la estructura ErrorResource (code, message, details).", value = "{\"code\":\"UNEXPECTED_ERROR\",\"message\":\"No se pudo completar la operación: asignar proveedor favorito\",\"details\":\"Ocurrió un error al procesar los datos de equipo en FuelBridge.\"}")))
    })
    @PostMapping("/{equipmentId}/favorite-provider")
    public ResponseEntity<EquipmentResource> assignFavoriteProvider(
            @PathVariable Long equipmentId, @RequestBody FavoriteProviderResource resource) {
        return equipmentRepository.findById(equipmentId)
                .map(equipment -> {
                    equipment.assignFavoriteProvider(resource.providerId());
                    var saved = equipmentRepository.save(equipment);
                    return ResponseEntity.ok(EquipmentResourceFromEntityAssembler.toResourceFromEntity(saved));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Registrar equipo",
        description = "Registra un equipo de la empresa compradora con su combustible, capacidad de tanque y configuración de recarga.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true, description = "Datos necesarios para registrar equipo.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.halofuel.fuelbridge.platform.equipment.interfaces.rest.resources.CreateEquipmentResource.class),
                examples = { @ExampleObject(name = "principal", value = "{\"name\":\"Excavadora Caterpillar 320\",\"equipmentType\":\"EXCAVATOR\",\"licensePlate\":\"EQ-320-LIM\",\"fuelType\":\"DIESEL\",\"tankCapacity\":400,\"currentLevel\":80,\"location\":\"Obra Villa El Salvador, Lima\",\"status\":\"ACTIVE\",\"autoRefill\":true,\"refillThreshold\":25,\"lastRefillDate\":\"2026-10-08\",\"companyId\":101,\"favoriteProviderId\":202}") }))
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Registro creado correctamente.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.halofuel.fuelbridge.platform.equipment.interfaces.rest.resources.EquipmentResource.class), examples = @ExampleObject(name = "respuesta", value = "{\"id\":305,\"name\":\"Excavadora Caterpillar 320\",\"equipmentType\":\"EXCAVATOR\",\"licensePlate\":\"EQ-320-LIM\",\"fuelType\":\"DIESEL\",\"tankCapacity\":400,\"currentLevel\":80,\"location\":\"Obra Villa El Salvador, Lima\",\"status\":\"ACTIVE\",\"autoRefill\":true,\"refillThreshold\":25,\"lastRefillDate\":\"2026-10-08\",\"companyId\":101,\"favoriteProviderId\":202}"))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida: revisar identificadores, campos o valores indicados en el cuerpo o los parámetros.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "error", summary = "Solicitud inválida para registrar equipo", description = "Ejemplo de error con la estructura ErrorResource (code, message, details).", value = "{\"code\":\"VALIDATION_ERROR\",\"message\":\"Solicitud inválida para registrar equipo\",\"details\":\"El equipo Excavadora CAT 320 debe incluir los datos obligatorios y pertenecer a la empresa compradora.\"}"))),
        @ApiResponse(responseCode = "401", description = "Falta un JWT Bearer válido, o el token ha expirado o tiene una firma incorrecta.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "Se requiere autenticación para acceder a FuelBridge", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El manejador de seguridad actual usa sendError; el cuerpo lo determina Spring.", value = "{\"code\":\"UNAUTHORIZED\",\"message\":\"Se requiere autenticación para acceder a FuelBridge\",\"details\":\"Envíe un JWT vigente emitido por FuelBridge en Authorization: Bearer <token> para registrar equipo.\"}"))),
        @ApiResponse(responseCode = "403", description = "Solicitud rechazada por la política de acceso o por un origen CORS no permitido.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "Acceso a FuelBridge no permitido", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El rechazo actual lo gestiona Spring Security o CORS y puede no incluir cuerpo.", value = "{\"code\":\"FORBIDDEN\",\"message\":\"Acceso a FuelBridge no permitido\",\"details\":\"La política de acceso o el origen CORS no permite registrar equipo.\"}"))),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "error", summary = "No se pudo completar la operación: registrar equipo", description = "Ejemplo de error con la estructura ErrorResource (code, message, details).", value = "{\"code\":\"UNEXPECTED_ERROR\",\"message\":\"No se pudo completar la operación: registrar equipo\",\"details\":\"Ocurrió un error al procesar los datos de equipo en FuelBridge.\"}")))
    })
    @PostMapping
    public ResponseEntity<?> createEquipment(@RequestBody CreateEquipmentResource resource) {
        var command = CreateEquipmentCommandFromResourceAssembler.toCommandFromResource(resource);
        var result = equipmentCommandService.handle(command);
        return ResponseEntityAssembler.toResponseEntityFromResult(
                result,
                EquipmentResourceFromEntityAssembler::toResourceFromEntity,
                HttpStatus.CREATED);
    }

    @Operation(
        summary = "Actualizar equipo",
        description = "Actualiza los datos del equipo existente sin cambiar su empresa propietaria.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = {
            @Parameter(name = "equipmentId", in = ParameterIn.PATH, required = true, description = "Identificador del equipo.", example = "305")
        },
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true, description = "Datos necesarios para actualizar equipo.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.halofuel.fuelbridge.platform.equipment.interfaces.rest.resources.UpdateEquipmentResource.class),
                examples = { @ExampleObject(name = "principal", value = "{\"name\":\"Excavadora Caterpillar 320\",\"equipmentType\":\"EXCAVATOR\",\"licensePlate\":\"EQ-320-LIM\",\"fuelType\":\"DIESEL\",\"tankCapacity\":400,\"currentLevel\":80,\"location\":\"Obra Villa El Salvador, Lima\",\"status\":\"ACTIVE\",\"autoRefill\":true,\"refillThreshold\":25,\"lastRefillDate\":\"2026-10-08\",\"favoriteProviderId\":202}") }))
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operación completada correctamente.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.halofuel.fuelbridge.platform.equipment.interfaces.rest.resources.EquipmentResource.class), examples = @ExampleObject(name = "respuesta", value = "{\"id\":305,\"name\":\"Excavadora Caterpillar 320\",\"equipmentType\":\"EXCAVATOR\",\"licensePlate\":\"EQ-320-LIM\",\"fuelType\":\"DIESEL\",\"tankCapacity\":400,\"currentLevel\":320,\"location\":\"Obra Villa El Salvador, Lima\",\"status\":\"ACTIVE\",\"autoRefill\":true,\"refillThreshold\":25,\"lastRefillDate\":\"2026-10-08\",\"companyId\":101,\"favoriteProviderId\":202}"))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida: revisar identificadores, campos o valores indicados en el cuerpo o los parámetros.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "error", summary = "Solicitud inválida para actualizar equipo", description = "Ejemplo de error con la estructura ErrorResource (code, message, details).", value = "{\"code\":\"VALIDATION_ERROR\",\"message\":\"Solicitud inválida para actualizar equipo\",\"details\":\"El equipo Excavadora CAT 320 debe incluir los datos obligatorios y pertenecer a la empresa compradora.\"}"))),
        @ApiResponse(responseCode = "401", description = "Falta un JWT Bearer válido, o el token ha expirado o tiene una firma incorrecta.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "Se requiere autenticación para acceder a FuelBridge", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El manejador de seguridad actual usa sendError; el cuerpo lo determina Spring.", value = "{\"code\":\"UNAUTHORIZED\",\"message\":\"Se requiere autenticación para acceder a FuelBridge\",\"details\":\"Envíe un JWT vigente emitido por FuelBridge en Authorization: Bearer <token> para actualizar equipo.\"}"))),
        @ApiResponse(responseCode = "403", description = "Solicitud rechazada por la política de acceso o por un origen CORS no permitido.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "Acceso a FuelBridge no permitido", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El rechazo actual lo gestiona Spring Security o CORS y puede no incluir cuerpo.", value = "{\"code\":\"FORBIDDEN\",\"message\":\"Acceso a FuelBridge no permitido\",\"details\":\"La política de acceso o el origen CORS no permite actualizar equipo.\"}"))),
        @ApiResponse(responseCode = "404", description = "No se encontró el recurso solicitado o una dependencia identificada por la operación.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "error", summary = "Equipo no encontrado", description = "Ejemplo de error con la estructura ErrorResource (code, message, details).", value = "{\"code\":\"EQUIPMENT_NOT_FOUND\",\"message\":\"Equipo no encontrado\",\"details\":\"No existe equipo con identificador 305.\"}"))),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "error", summary = "No se pudo completar la operación: actualizar equipo", description = "Ejemplo de error con la estructura ErrorResource (code, message, details).", value = "{\"code\":\"UNEXPECTED_ERROR\",\"message\":\"No se pudo completar la operación: actualizar equipo\",\"details\":\"Ocurrió un error al procesar los datos de equipo en FuelBridge.\"}")))
    })
    @PostMapping("/{equipmentId}/update")
    public ResponseEntity<?> updateEquipment(@PathVariable Long equipmentId,
                                             @RequestBody UpdateEquipmentResource resource) {
        var command = UpdateEquipmentCommandFromResourceAssembler.toCommandFromResource(equipmentId, resource);
        var result = equipmentCommandService.handle(command);
        return ResponseEntityAssembler.toResponseEntityFromResult(
                result,
                EquipmentResourceFromEntityAssembler::toResourceFromEntity,
                HttpStatus.OK);
    }

    @Operation(
        summary = "Listar equipos",
        description = "Devuelve todos los equipos registrados. La lista está vacía cuando no hay equipos.",
        security = { @SecurityRequirement(name = "bearerAuth") }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operación completada correctamente.", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = com.halofuel.fuelbridge.platform.equipment.interfaces.rest.resources.EquipmentResource.class)), examples = @ExampleObject(name = "respuesta", value = "[{\"id\":305,\"name\":\"Excavadora Caterpillar 320\",\"equipmentType\":\"EXCAVATOR\",\"licensePlate\":\"EQ-320-LIM\",\"fuelType\":\"DIESEL\",\"tankCapacity\":400,\"currentLevel\":80,\"location\":\"Obra Villa El Salvador, Lima\",\"status\":\"ACTIVE\",\"autoRefill\":true,\"refillThreshold\":25,\"lastRefillDate\":\"2026-10-08\",\"companyId\":101,\"favoriteProviderId\":202}]"))),
        @ApiResponse(responseCode = "401", description = "Falta un JWT Bearer válido, o el token ha expirado o tiene una firma incorrecta.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "Se requiere autenticación para acceder a FuelBridge", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El manejador de seguridad actual usa sendError; el cuerpo lo determina Spring.", value = "{\"code\":\"UNAUTHORIZED\",\"message\":\"Se requiere autenticación para acceder a FuelBridge\",\"details\":\"Envíe un JWT vigente emitido por FuelBridge en Authorization: Bearer <token> para listar equipos.\"}"))),
        @ApiResponse(responseCode = "403", description = "Solicitud rechazada por la política de acceso o por un origen CORS no permitido.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "Acceso a FuelBridge no permitido", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El rechazo actual lo gestiona Spring Security o CORS y puede no incluir cuerpo.", value = "{\"code\":\"FORBIDDEN\",\"message\":\"Acceso a FuelBridge no permitido\",\"details\":\"La política de acceso o el origen CORS no permite listar equipos.\"}"))),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "error", summary = "No se pudo completar la operación: listar equipos", description = "Ejemplo de error con la estructura ErrorResource (code, message, details).", value = "{\"code\":\"UNEXPECTED_ERROR\",\"message\":\"No se pudo completar la operación: listar equipos\",\"details\":\"Ocurrió un error al procesar los datos de equipo en FuelBridge.\"}")))
    })
    @GetMapping
    public ResponseEntity<List<EquipmentResource>> getAllEquipment() {
        var equipment = equipmentQueryService.handle(new GetAllEquipmentQuery());
        var resources = equipment.stream().map(EquipmentResourceFromEntityAssembler::toResourceFromEntity).toList();
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    @Operation(
        summary = "Consultar equipo",
        description = "Devuelve el equipo identificado, incluida su configuración de recarga automática y proveedor favorito.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = {
            @Parameter(name = "equipmentId", in = ParameterIn.PATH, required = true, description = "Identificador del equipo.", example = "305")
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operación completada correctamente.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.halofuel.fuelbridge.platform.equipment.interfaces.rest.resources.EquipmentResource.class), examples = @ExampleObject(name = "respuesta", value = "{\"id\":305,\"name\":\"Excavadora Caterpillar 320\",\"equipmentType\":\"EXCAVATOR\",\"licensePlate\":\"EQ-320-LIM\",\"fuelType\":\"DIESEL\",\"tankCapacity\":400,\"currentLevel\":80,\"location\":\"Obra Villa El Salvador, Lima\",\"status\":\"ACTIVE\",\"autoRefill\":true,\"refillThreshold\":25,\"lastRefillDate\":\"2026-10-08\",\"companyId\":101,\"favoriteProviderId\":202}"))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida: revisar identificadores, campos o valores indicados en el cuerpo o los parámetros.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "error", summary = "Solicitud inválida para consultar equipo", description = "Ejemplo de error con la estructura ErrorResource (code, message, details).", value = "{\"code\":\"VALIDATION_ERROR\",\"message\":\"Solicitud inválida para consultar equipo\",\"details\":\"El parámetro equipmentId=abc no es un identificador numérico válido de equipo.\"}"))),
        @ApiResponse(responseCode = "401", description = "Falta un JWT Bearer válido, o el token ha expirado o tiene una firma incorrecta.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "Se requiere autenticación para acceder a FuelBridge", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El manejador de seguridad actual usa sendError; el cuerpo lo determina Spring.", value = "{\"code\":\"UNAUTHORIZED\",\"message\":\"Se requiere autenticación para acceder a FuelBridge\",\"details\":\"Envíe un JWT vigente emitido por FuelBridge en Authorization: Bearer <token> para consultar equipo.\"}"))),
        @ApiResponse(responseCode = "403", description = "Solicitud rechazada por la política de acceso o por un origen CORS no permitido.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "Acceso a FuelBridge no permitido", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El rechazo actual lo gestiona Spring Security o CORS y puede no incluir cuerpo.", value = "{\"code\":\"FORBIDDEN\",\"message\":\"Acceso a FuelBridge no permitido\",\"details\":\"La política de acceso o el origen CORS no permite consultar equipo.\"}"))),
        @ApiResponse(responseCode = "404", description = "No se encontró el recurso solicitado o una dependencia identificada por la operación.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "Equipo no encontrado", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El controlador actual puede devolver 404 sin cuerpo.", value = "{\"code\":\"EQUIPMENT_NOT_FOUND\",\"message\":\"Equipo no encontrado\",\"details\":\"No existe equipo con identificador 305.\"}"))),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "error", summary = "No se pudo completar la operación: consultar equipo", description = "Ejemplo de error con la estructura ErrorResource (code, message, details).", value = "{\"code\":\"UNEXPECTED_ERROR\",\"message\":\"No se pudo completar la operación: consultar equipo\",\"details\":\"Ocurrió un error al procesar los datos de equipo en FuelBridge.\"}")))
    })
    @GetMapping("/{equipmentId}")
    public ResponseEntity<EquipmentResource> getEquipmentById(@PathVariable Long equipmentId) {
        var result = equipmentQueryService.handle(new GetEquipmentByIdQuery(equipmentId));
        return result.map(e -> new ResponseEntity<>(
                        EquipmentResourceFromEntityAssembler.toResourceFromEntity(e), HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @Operation(
        summary = "Listar equipos de una empresa",
        description = "Devuelve los equipos de la empresa compradora indicada; devuelve una lista vacía si no tiene equipos.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = {
            @Parameter(name = "companyId", in = ParameterIn.PATH, required = true, description = "Identificador de la empresa compradora.", example = "101")
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operación completada correctamente.", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = com.halofuel.fuelbridge.platform.equipment.interfaces.rest.resources.EquipmentResource.class)), examples = @ExampleObject(name = "respuesta", value = "[{\"id\":305,\"name\":\"Excavadora Caterpillar 320\",\"equipmentType\":\"EXCAVATOR\",\"licensePlate\":\"EQ-320-LIM\",\"fuelType\":\"DIESEL\",\"tankCapacity\":400,\"currentLevel\":80,\"location\":\"Obra Villa El Salvador, Lima\",\"status\":\"ACTIVE\",\"autoRefill\":true,\"refillThreshold\":25,\"lastRefillDate\":\"2026-10-08\",\"companyId\":101,\"favoriteProviderId\":202}]"))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida: revisar identificadores, campos o valores indicados en el cuerpo o los parámetros.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "error", summary = "Solicitud inválida para listar equipos de una empresa", description = "Ejemplo de error con la estructura ErrorResource (code, message, details).", value = "{\"code\":\"VALIDATION_ERROR\",\"message\":\"Solicitud inválida para listar equipos de una empresa\",\"details\":\"El parámetro companyId=abc no es un identificador numérico válido de equipo.\"}"))),
        @ApiResponse(responseCode = "401", description = "Falta un JWT Bearer válido, o el token ha expirado o tiene una firma incorrecta.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "Se requiere autenticación para acceder a FuelBridge", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El manejador de seguridad actual usa sendError; el cuerpo lo determina Spring.", value = "{\"code\":\"UNAUTHORIZED\",\"message\":\"Se requiere autenticación para acceder a FuelBridge\",\"details\":\"Envíe un JWT vigente emitido por FuelBridge en Authorization: Bearer <token> para listar equipos de una empresa.\"}"))),
        @ApiResponse(responseCode = "403", description = "Solicitud rechazada por la política de acceso o por un origen CORS no permitido.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "Acceso a FuelBridge no permitido", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El rechazo actual lo gestiona Spring Security o CORS y puede no incluir cuerpo.", value = "{\"code\":\"FORBIDDEN\",\"message\":\"Acceso a FuelBridge no permitido\",\"details\":\"La política de acceso o el origen CORS no permite listar equipos de una empresa.\"}"))),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "error", summary = "No se pudo completar la operación: listar equipos de una empresa", description = "Ejemplo de error con la estructura ErrorResource (code, message, details).", value = "{\"code\":\"UNEXPECTED_ERROR\",\"message\":\"No se pudo completar la operación: listar equipos de una empresa\",\"details\":\"Ocurrió un error al procesar los datos de equipo en FuelBridge.\"}")))
    })
    @GetMapping("/company/{companyId}")
    public ResponseEntity<List<EquipmentResource>> getEquipmentByCompany(@PathVariable Long companyId) {
        var equipment = equipmentQueryService.handle(new GetEquipmentByCompanyIdQuery(companyId));
        var resources = equipment.stream().map(EquipmentResourceFromEntityAssembler::toResourceFromEntity).toList();
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }
}
