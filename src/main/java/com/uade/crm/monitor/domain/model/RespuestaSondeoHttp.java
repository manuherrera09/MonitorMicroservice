package com.uade.crm.monitor.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Resultado crudo del sondeo HTTP hacia un endpoint de un microservicio.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RespuestaSondeoHttp {

    private boolean alcanzable;
    private int codigoHttp;
    private long latenciaMs;
    private String cuerpo;
    private String urlConsultada;
    private String mensajeError;
}
