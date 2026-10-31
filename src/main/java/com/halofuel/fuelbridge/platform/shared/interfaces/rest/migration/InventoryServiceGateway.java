package com.halofuel.fuelbridge.platform.shared.interfaces.rest.migration;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.halofuel.fuelbridge.platform.shared.infrastructure.http.InventoryServiceClient;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.bind.annotation.*;

/** Retains the public fuel-product routes while the standalone Inventory service owns the BC. */
@RestController
@RequestMapping("/api/v1/fuel-products")
@Tag(name = "Fuel Products", description = "Gateway de compatibilidad hacia el microservicio Inventory")
public class InventoryServiceGateway {
    private static final String BASE = "/api/v1/fuel-products";
    private final InventoryServiceClient client;

    public InventoryServiceGateway(InventoryServiceClient client) {
        this.client = client;
    }

    @Operation(
        summary = "Listar productos de combustible",
        description = "Reenvía al microservicio Inventory la consulta de todos los productos registrados.",
        security = { @SecurityRequirement(name = "bearerAuth") }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de productos.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "respuesta", value = "[{\"id\":303,\"name\":\"Diésel B5\",\"fuelType\":\"DIESEL\",\"pricePerUnit\":16.5,\"unit\":\"GAL\",\"availableStock\":1000,\"capacity\":20000,\"providerId\":202,\"active\":true}]"))),
        @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado.", content = @Content),
        @ApiResponse(responseCode = "502", description = "El microservicio Inventory no está disponible.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "servicioNoDisponible", value = "{\"message\":\"Standalone service unavailable\"}"))),
        @ApiResponse(responseCode = "default", description = "Otro código transmitido por el microservicio; se conserva su cuerpo y tipo de contenido.", content = @Content)
    })
    @GetMapping
    public ResponseEntity<byte[]> getAll(HttpServletRequest request) {
        return client.forward(HttpMethod.GET, BASE, new LinkedMultiValueMap<>(), null, request);
    }

    @Operation(
        summary = "Consultar producto de combustible",
        description = "Reenvía al microservicio Inventory la consulta del producto identificado.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = {
            @Parameter(name = "fuelProductId", in = ParameterIn.PATH, required = true, description = "Identificador del producto de combustible.", example = "303")
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Producto encontrado.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "respuesta", value = "{\"id\":303,\"name\":\"Diésel B5\",\"fuelType\":\"DIESEL\",\"pricePerUnit\":16.5,\"unit\":\"GAL\",\"availableStock\":1000,\"capacity\":20000,\"providerId\":202,\"active\":true}"))),
        @ApiResponse(responseCode = "404", description = "No existe el producto indicado.", content = @Content),
        @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado.", content = @Content),
        @ApiResponse(responseCode = "502", description = "El microservicio Inventory no está disponible.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "servicioNoDisponible", value = "{\"message\":\"Standalone service unavailable\"}"))),
        @ApiResponse(responseCode = "default", description = "Otro código transmitido por el microservicio; se conserva su cuerpo y tipo de contenido.", content = @Content)
    })
    @GetMapping("/{fuelProductId}")
    public ResponseEntity<byte[]> getById(@PathVariable Long fuelProductId, HttpServletRequest request) {
        return client.forward(HttpMethod.GET, BASE + "/" + fuelProductId, new LinkedMultiValueMap<>(), null, request);
    }

    @Operation(
        summary = "Listar productos de un proveedor",
        description = "Reenvía al microservicio Inventory la consulta de los productos del proveedor indicado.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = {
            @Parameter(name = "providerId", in = ParameterIn.PATH, required = true, description = "Identificador de la empresa proveedora.", example = "202")
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Productos del proveedor.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "respuesta", value = "[{\"id\":303,\"name\":\"Diésel B5\",\"fuelType\":\"DIESEL\",\"pricePerUnit\":16.5,\"unit\":\"GAL\",\"availableStock\":1000,\"capacity\":20000,\"providerId\":202,\"active\":true}]"))),
        @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado.", content = @Content),
        @ApiResponse(responseCode = "502", description = "El microservicio Inventory no está disponible.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "servicioNoDisponible", value = "{\"message\":\"Standalone service unavailable\"}"))),
        @ApiResponse(responseCode = "default", description = "Otro código transmitido por el microservicio; se conserva su cuerpo y tipo de contenido.", content = @Content)
    })
    @GetMapping("/provider/{providerId}")
    public ResponseEntity<byte[]> getByProvider(@PathVariable Long providerId, HttpServletRequest request) {
        return client.forward(HttpMethod.GET, BASE + "/provider/" + providerId, new LinkedMultiValueMap<>(), null, request);
    }

    @Operation(
        summary = "Registrar producto de combustible",
        description = "Reenvía al microservicio Inventory el registro de un producto con precio, unidad, stock y capacidad.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true, description = "Datos del producto a registrar.",
            content = @Content(mediaType = "application/json", schema = @Schema(type = "object"),
                examples = { @ExampleObject(name = "principal", value = "{\"name\":\"Diésel B5\",\"fuelType\":\"DIESEL\",\"pricePerUnit\":16.5,\"unit\":\"GAL\",\"availableStock\":1000,\"capacity\":20000,\"providerId\":202,\"active\":true}") }))
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Producto registrado.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "respuesta", value = "{\"id\":303,\"name\":\"Diésel B5\",\"fuelType\":\"DIESEL\",\"pricePerUnit\":16.5,\"unit\":\"GAL\",\"availableStock\":1000,\"capacity\":20000,\"providerId\":202,\"active\":true}"))),
        @ApiResponse(responseCode = "400", description = "Cuerpo o parámetros inválidos (por ejemplo, stock negativo o no numérico).", content = @Content),
        @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado.", content = @Content),
        @ApiResponse(responseCode = "502", description = "El microservicio Inventory no está disponible.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "servicioNoDisponible", value = "{\"message\":\"Standalone service unavailable\"}"))),
        @ApiResponse(responseCode = "default", description = "Otro código transmitido por el microservicio; se conserva su cuerpo y tipo de contenido.", content = @Content)
    })
    @PostMapping
    public ResponseEntity<byte[]> create(@RequestBody byte[] body, HttpServletRequest request) {
        return client.forward(HttpMethod.POST, BASE, new LinkedMultiValueMap<>(), body, request);
    }

    @Operation(
        summary = "Actualizar stock",
        description = "Reenvía al microservicio Inventory el nuevo stock disponible; debe ser un número mayor o igual a cero.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = {
            @Parameter(name = "fuelProductId", in = ParameterIn.PATH, required = true, description = "Identificador del producto de combustible.", example = "303")
        },
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true, description = "Nuevo stock disponible.",
            content = @Content(mediaType = "application/json", schema = @Schema(type = "object"),
                examples = { @ExampleObject(name = "principal", value = "{\"newStock\":1500}") }))
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Stock actualizado.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "respuesta", value = "{\"id\":303,\"name\":\"Diésel B5\",\"fuelType\":\"DIESEL\",\"pricePerUnit\":16.5,\"unit\":\"GAL\",\"availableStock\":1000,\"capacity\":20000,\"providerId\":202,\"active\":true}"))),
        @ApiResponse(responseCode = "400", description = "Cuerpo o parámetros inválidos (por ejemplo, stock negativo o no numérico).", content = @Content),
        @ApiResponse(responseCode = "404", description = "No existe el producto indicado.", content = @Content),
        @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado.", content = @Content),
        @ApiResponse(responseCode = "502", description = "El microservicio Inventory no está disponible.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "servicioNoDisponible", value = "{\"message\":\"Standalone service unavailable\"}"))),
        @ApiResponse(responseCode = "default", description = "Otro código transmitido por el microservicio; se conserva su cuerpo y tipo de contenido.", content = @Content)
    })
    @PostMapping("/{fuelProductId}/update-stock")
    public ResponseEntity<byte[]> updateStock(@PathVariable Long fuelProductId, @RequestBody byte[] body,
                                              HttpServletRequest request) {
        return client.forward(HttpMethod.POST, BASE + "/" + fuelProductId + "/update-stock", new LinkedMultiValueMap<>(), body, request);
    }

    @Operation(
        summary = "Actualizar producto de combustible",
        description = "Reenvía al microservicio Inventory la actualización completa del producto sin cambiar su proveedor.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = {
            @Parameter(name = "fuelProductId", in = ParameterIn.PATH, required = true, description = "Identificador del producto de combustible.", example = "303")
        },
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true, description = "Datos actualizados del producto.",
            content = @Content(mediaType = "application/json", schema = @Schema(type = "object"),
                examples = { @ExampleObject(name = "principal", value = "{\"name\":\"Diésel B5\",\"fuelType\":\"DIESEL\",\"pricePerUnit\":16.5,\"unit\":\"GAL\",\"availableStock\":1000,\"capacity\":20000,\"active\":true}") }))
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Producto actualizado.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "respuesta", value = "{\"id\":303,\"name\":\"Diésel B5\",\"fuelType\":\"DIESEL\",\"pricePerUnit\":16.5,\"unit\":\"GAL\",\"availableStock\":1000,\"capacity\":20000,\"providerId\":202,\"active\":true}"))),
        @ApiResponse(responseCode = "400", description = "Cuerpo o parámetros inválidos (por ejemplo, stock negativo o no numérico).", content = @Content),
        @ApiResponse(responseCode = "404", description = "No existe el producto indicado.", content = @Content),
        @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado.", content = @Content),
        @ApiResponse(responseCode = "502", description = "El microservicio Inventory no está disponible.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "servicioNoDisponible", value = "{\"message\":\"Standalone service unavailable\"}"))),
        @ApiResponse(responseCode = "default", description = "Otro código transmitido por el microservicio; se conserva su cuerpo y tipo de contenido.", content = @Content)
    })
    @PutMapping("/{fuelProductId}")
    public ResponseEntity<byte[]> update(@PathVariable Long fuelProductId, @RequestBody byte[] body,
                                         HttpServletRequest request) {
        return client.forward(HttpMethod.PUT, BASE + "/" + fuelProductId, new LinkedMultiValueMap<>(), body, request);
    }

    @Operation(
        summary = "Eliminar producto de combustible",
        description = "Reenvía al microservicio Inventory la eliminación del producto identificado.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = {
            @Parameter(name = "fuelProductId", in = ParameterIn.PATH, required = true, description = "Identificador del producto de combustible.", example = "303")
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Producto eliminado; sin cuerpo de respuesta.", content = @Content),
        @ApiResponse(responseCode = "404", description = "No existe el producto indicado.", content = @Content),
        @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado.", content = @Content),
        @ApiResponse(responseCode = "502", description = "El microservicio Inventory no está disponible.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "servicioNoDisponible", value = "{\"message\":\"Standalone service unavailable\"}"))),
        @ApiResponse(responseCode = "default", description = "Otro código transmitido por el microservicio; se conserva su cuerpo y tipo de contenido.", content = @Content)
    })
    @DeleteMapping("/{fuelProductId}")
    public ResponseEntity<byte[]> delete(@PathVariable Long fuelProductId, HttpServletRequest request) {
        return client.forward(HttpMethod.DELETE, BASE + "/" + fuelProductId, new LinkedMultiValueMap<>(), null, request);
    }
}
