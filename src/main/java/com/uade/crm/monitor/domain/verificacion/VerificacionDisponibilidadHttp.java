package com.uade.crm.monitor.domain.verificacion;

import com.uade.crm.monitor.domain.model.ObjetivoMonitoreo;
import com.uade.crm.monitor.domain.model.RespuestaSondeoHttp;
import com.uade.crm.monitor.domain.model.ResultadoVerificacion;
import com.uade.crm.monitor.domain.model.TipoVerificacion;
import com.uade.crm.monitor.domain.port.ClienteHttpSaludPort;

/**
 * Estrategia concreta (Strategy) para microservicios que no usan Spring Boot Actuator
 * (por ejemplo Leads en Python/FastAPI o verificación de contrato OpenAPI).
 */
public class VerificacionDisponibilidadHttp extends VerificacionSalud {

    private final ClienteHttpSaludPort clienteHttp;

    public VerificacionDisponibilidadHttp(ClienteHttpSaludPort clienteHttp) {
        super(clienteHttp);
        this.clienteHttp = clienteHttp;
    }

    @Override
    public ResultadoVerificacion verificar(ObjetivoMonitoreo objetivo) {
        String ruta = objetivo.getRutaAlternativa() != null && !objetivo.getRutaAlternativa().isBlank()
                ? objetivo.getRutaAlternativa()
                : (objetivo.getRutaSalud() != null && !objetivo.getRutaSalud().isBlank()
                    ? objetivo.getRutaSalud()
                    : "/openapi.json");

        RespuestaSondeoHttp sondeo = clienteHttp.sondearGet(objetivo.getUrlBase(), ruta);
        return evaluarRespuesta(objetivo, sondeo, TipoVerificacion.HTTP_DISPONIBILIDAD);
    }
}
