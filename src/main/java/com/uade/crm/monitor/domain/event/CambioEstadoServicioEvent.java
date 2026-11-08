package com.uade.crm.monitor.domain.event;

import com.uade.crm.monitor.domain.model.EstadoSalud;
import com.uade.crm.monitor.domain.model.ResultadoVerificacion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Evento de dominio emitido cuando un microservicio cambia de estado
 * (ej. de UP a DOWN o de DOWN a UP). Implementa el patrón Observer.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CambioEstadoServicioEvent {

    private String servicio;
    private EstadoSalud estadoAnterior;
    private EstadoSalud estadoNuevo;
    private long latenciaMs;
    private String detalle;
    private LocalDateTime fechaCambio;
    private ResultadoVerificacion verificacion;
}
