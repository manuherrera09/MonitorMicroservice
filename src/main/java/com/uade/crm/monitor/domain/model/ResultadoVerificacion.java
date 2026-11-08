package com.uade.crm.monitor.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entidad de dominio que representa el resultado de una verificación de salud
 * de un microservicio en un instante dado.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResultadoVerificacion {

    private Long id;
    private String servicio;
    private String nombreMostrar;
    private String urlConsultada;
    private EstadoSalud estado;
    private long latenciaMs;
    private LocalDateTime ultimaVerificacion;
    private String detalle;
    private Integer codigoHttp;
    private TipoVerificacion tipoVerificacion;
}
