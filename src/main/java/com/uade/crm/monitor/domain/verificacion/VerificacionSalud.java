package com.uade.crm.monitor.domain.verificacion;

import com.uade.crm.monitor.domain.model.EstadoSalud;
import com.uade.crm.monitor.domain.model.ObjetivoMonitoreo;
import com.uade.crm.monitor.domain.model.RespuestaSondeoHttp;
import com.uade.crm.monitor.domain.model.ResultadoVerificacion;
import com.uade.crm.monitor.domain.model.TipoVerificacion;
import com.uade.crm.monitor.domain.port.ClienteHttpSaludPort;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

/**
 * Estrategia concreta (Strategy) que verifica el endpoint de salud de un microservicio
 * (por ejemplo Spring Boot Actuator /actuator/health) y evalúa código HTTP,
 * estado reportado en el JSON y umbral de latencia.
 */
@RequiredArgsConstructor
public class VerificacionSalud implements Verificacion {

    private final ClienteHttpSaludPort clienteHttp;

    @Override
    public ResultadoVerificacion verificar(ObjetivoMonitoreo objetivo) {
        String rutaPrimaria = objetivo.getRutaSalud() != null && !objetivo.getRutaSalud().isBlank()
                ? objetivo.getRutaSalud()
                : "/actuator/health";

        RespuestaSondeoHttp sondeo = clienteHttp.sondearGet(objetivo.getUrlBase(), rutaPrimaria);

        // Si el servicio no expone /actuator/health (404) pero tiene ruta alternativa configurada
        // (ej. /v3/api-docs en Spring o /openapi.json en FastAPI), probamos la ruta alternativa.
        if (sondeo.isAlcanzable()
                && sondeo.getCodigoHttp() == 404
                && objetivo.getRutaAlternativa() != null
                && !objetivo.getRutaAlternativa().isBlank()) {
            sondeo = clienteHttp.sondearGet(objetivo.getUrlBase(), objetivo.getRutaAlternativa());
        }

        return evaluarRespuesta(objetivo, sondeo, TipoVerificacion.ACTUATOR_HEALTH);
    }

    protected ResultadoVerificacion evaluarRespuesta(
            ObjetivoMonitoreo objetivo,
            RespuestaSondeoHttp sondeo,
            TipoVerificacion tipo) {

        LocalDateTime ahora = LocalDateTime.now();

        if (!sondeo.isAlcanzable()) {
            return ResultadoVerificacion.builder()
                    .servicio(objetivo.getServicio())
                    .nombreMostrar(objetivo.getNombreMostrar())
                    .urlConsultada(sondeo.getUrlConsultada())
                    .estado(EstadoSalud.DOWN)
                    .latenciaMs(sondeo.getLatenciaMs())
                    .ultimaVerificacion(ahora)
                    .codigoHttp(null)
                    .tipoVerificacion(tipo)
                    .detalle("Sin conexión: " + (sondeo.getMensajeError() != null ? sondeo.getMensajeError() : "Servicio inalcanzable"))
                    .build();
        }

        int statusHttp = sondeo.getCodigoHttp();
        long latencia = sondeo.getLatenciaMs();
        String cuerpo = sondeo.getCuerpo() != null ? sondeo.getCuerpo() : "";

        // Si responde 5xx o el cuerpo de Actuator reporta explícitamente DOWN / OUT_OF_SERVICE
        if (statusHttp >= 500 || contieneEstadoDown(cuerpo)) {
            return ResultadoVerificacion.builder()
                    .servicio(objetivo.getServicio())
                    .nombreMostrar(objetivo.getNombreMostrar())
                    .urlConsultada(sondeo.getUrlConsultada())
                    .estado(EstadoSalud.DOWN)
                    .latenciaMs(latencia)
                    .ultimaVerificacion(ahora)
                    .codigoHttp(statusHttp)
                    .tipoVerificacion(tipo)
                    .detalle("Servicio reporta fallo (HTTP " + statusHttp + ")")
                    .build();
        }

        // Si responde 4xx
        if (statusHttp >= 400) {
            return ResultadoVerificacion.builder()
                    .servicio(objetivo.getServicio())
                    .nombreMostrar(objetivo.getNombreMostrar())
                    .urlConsultada(sondeo.getUrlConsultada())
                    .estado(EstadoSalud.DEGRADADO)
                    .latenciaMs(latencia)
                    .ultimaVerificacion(ahora)
                    .codigoHttp(statusHttp)
                    .tipoVerificacion(tipo)
                    .detalle("Endpoint de salud devolvió HTTP " + statusHttp)
                    .build();
        }

        // Si responde 2xx pero supera el umbral de latencia degradada
        long umbralDegradado = objetivo.getUmbralDegradadoMs() > 0 ? objetivo.getUmbralDegradadoMs() : 1000L;
        if (latencia >= umbralDegradado) {
            return ResultadoVerificacion.builder()
                    .servicio(objetivo.getServicio())
                    .nombreMostrar(objetivo.getNombreMostrar())
                    .urlConsultada(sondeo.getUrlConsultada())
                    .estado(EstadoSalud.DEGRADADO)
                    .latenciaMs(latencia)
                    .ultimaVerificacion(ahora)
                    .codigoHttp(statusHttp)
                    .tipoVerificacion(tipo)
                    .detalle("Respuesta lenta (" + latencia + " ms >= umbral de " + umbralDegradado + " ms)")
                    .build();
        }

        return ResultadoVerificacion.builder()
                .servicio(objetivo.getServicio())
                .nombreMostrar(objetivo.getNombreMostrar())
                .urlConsultada(sondeo.getUrlConsultada())
                .estado(EstadoSalud.UP)
                .latenciaMs(latencia)
                .ultimaVerificacion(ahora)
                .codigoHttp(statusHttp)
                .tipoVerificacion(tipo)
                .detalle("Operativo (HTTP " + statusHttp + " en " + latencia + " ms)")
                .build();
    }

    private boolean contieneEstadoDown(String cuerpo) {
        String normalizado = cuerpo.toUpperCase().replace(" ", "");
        return normalizado.contains("\"STATUS\":\"DOWN\"")
                || normalizado.contains("\"STATUS\":\"OUT_OF_SERVICE\"");
    }
}
