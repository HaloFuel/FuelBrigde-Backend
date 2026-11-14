package com.halofuel.fuelbridge.platform.ordering.application.internal.commandservices;

import com.halofuel.fuelbridge.platform.ordering.application.outboundservices.FuelProductCatalog;
import com.halofuel.fuelbridge.platform.ordering.domain.model.exceptions.IllegalOrderStateTransitionException;
import com.halofuel.fuelbridge.platform.ordering.domain.model.valueobjects.RequestStatus;
import com.halofuel.fuelbridge.platform.ordering.domain.repositories.FuelOrderRepository;
import com.halofuel.fuelbridge.platform.ordering.infrastructure.persistence.jpa.entities.FuelRequestPersistenceEntity;
import com.halofuel.fuelbridge.platform.ordering.infrastructure.persistence.jpa.repositories.FuelRequestPersistenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @summary Pruebas unitarias de las guardas de estado de FuelRequestService.
 * @remarks Verifica que aceptar y rechazar solo sean posibles desde PENDING y que los demás estados
 * lancen IllegalOrderStateTransitionException sin persistir cambios.
 * @author HaloFuel
 */
class FuelRequestServiceTest {

    private FuelRequestPersistenceRepository requests;
    private FuelOrderRepository orders;
    private FuelRequestService service;

    @BeforeEach
    void setUp() {
        requests = mock(FuelRequestPersistenceRepository.class);
        orders = mock(FuelOrderRepository.class);
        service = new FuelRequestService(requests, mock(FuelProductCatalog.class), orders);
    }

    private FuelRequestPersistenceEntity requestIn(RequestStatus status) {
        var request = new FuelRequestPersistenceEntity();
        request.setStatus(status);
        when(requests.findById(1L)).thenReturn(Optional.of(request));
        when(requests.save(any())).thenAnswer(inv -> inv.getArgument(0));
        return request;
    }

    @Test
    void rejectFromPendingMovesToRejected() {
        var request = requestIn(RequestStatus.PENDING);

        var result = service.reject(1L, "Sin disponibilidad");

        assertThat(result.getStatus()).isEqualTo(RequestStatus.REJECTED);
        assertThat(request.getRejectionReason()).isEqualTo("Sin disponibilidad");
    }

    @ParameterizedTest
    @EnumSource(value = RequestStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "PENDING")
    void rejectIsRejectedFromEveryOtherStatus(RequestStatus from) {
        requestIn(from);

        assertThatThrownBy(() -> service.reject(1L, "motivo"))
                .isInstanceOf(IllegalOrderStateTransitionException.class)
                .hasMessage("Cannot transition from %s to REJECTED".formatted(from));
        verify(requests, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = RequestStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "PENDING")
    void acceptIsRejectedFromEveryOtherStatus(RequestStatus from) {
        requestIn(from);

        assertThatThrownBy(() -> service.accept(1L))
                .isInstanceOf(IllegalOrderStateTransitionException.class)
                .hasMessage("Cannot transition from %s to APPROVED".formatted(from));
        verify(requests, never()).save(any());
        verify(orders, never()).save(any());
    }
}
