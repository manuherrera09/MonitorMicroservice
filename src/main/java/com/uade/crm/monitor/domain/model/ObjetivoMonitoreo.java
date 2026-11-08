package com.uade.crm.monitor.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Representa la configuración de un microservicio objetivo a monitorear.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObjetivoMonitoreo {

    private String servicio;
    private String nombreMostrar;
    private String urlBase;
    private String rutaSalud;
    private String rutaAlternativa;
    private TipoVerificacion tipoVerificacion;
    private long umbralDegradadoMs;
}
