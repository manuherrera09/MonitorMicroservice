package com.uade.crm.monitor.api.dto;

import com.uade.crm.monitor.domain.model.EstadoSalud;
import com.uade.crm.monitor.domain.model.TipoVerificacion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstadoServicioResponse {

    private Long id;
    private String servicio;
    private String nombreMostrar;
    private EstadoSalud estado;
    private long latenciaMs;
    private LocalDateTime ultimaVerificacion;
    private String detalle;
    private String urlConsultada;
    private Integer codigoHttp;
    private TipoVerificacion tipoVerificacion;
}
