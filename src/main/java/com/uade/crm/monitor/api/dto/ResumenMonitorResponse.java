package com.uade.crm.monitor.api.dto;

import com.uade.crm.monitor.domain.model.EstadoSalud;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumenMonitorResponse {

    private EstadoSalud estadoGlobal;
    private int totalServicios;
    private int serviciosUp;
    private int serviciosDegradados;
    private int serviciosDown;
    private long latenciaPromedioMs;
    private LocalDateTime ultimaVerificacion;
    private List<EstadoServicioResponse> servicios;
}
