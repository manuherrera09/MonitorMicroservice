package com.uade.crm.monitor.domain.verificacion;

import com.uade.crm.monitor.domain.model.ObjetivoMonitoreo;
import com.uade.crm.monitor.domain.model.TipoVerificacion;
import com.uade.crm.monitor.domain.port.ClienteHttpSaludPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Implementación del Patrón Factory:
 * Instancia y parametriza la estrategia de verificación (simple o compuesta)
 * correspondiente según el tipo de microservicio objetivo.
 */
@Component
@RequiredArgsConstructor
public class VerificacionFactory {

    private final ClienteHttpSaludPort clienteHttpSaludPort;

    public Verificacion crearVerificacion(ObjetivoMonitoreo objetivo) {
        TipoVerificacion tipo = objetivo.getTipoVerificacion() != null
                ? objetivo.getTipoVerificacion()
                : TipoVerificacion.ACTUATOR_HEALTH;

        return switch (tipo) {
            case HTTP_DISPONIBILIDAD -> new VerificacionDisponibilidadHttp(clienteHttpSaludPort);
            case COMPUESTA -> new VerificacionCompuesta(List.of(
                    new VerificacionSalud(clienteHttpSaludPort),
                    new VerificacionDisponibilidadHttp(clienteHttpSaludPort)
            ));
            case ACTUATOR_HEALTH -> new VerificacionSalud(clienteHttpSaludPort);
        };
    }
}
