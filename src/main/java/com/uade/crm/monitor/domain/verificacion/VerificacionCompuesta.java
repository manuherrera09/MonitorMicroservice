package com.uade.crm.monitor.domain.verificacion;

import com.uade.crm.monitor.domain.model.EstadoSalud;
import com.uade.crm.monitor.domain.model.ObjetivoMonitoreo;
import com.uade.crm.monitor.domain.model.ResultadoVerificacion;
import com.uade.crm.monitor.domain.model.TipoVerificacion;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación del Patrón Composite (según docs/contratos-backend.md):
 * Permite tratar un conjunto de chequeos como si fuera una sola Verificacion,
 * consolidando la latencia y reportando el primer eslabón que falla o se degrada.
 */
public class VerificacionCompuesta implements Verificacion {

    private final List<Verificacion> verificaciones = new ArrayList<>();

    public VerificacionCompuesta(List<Verificacion> verificaciones) {
        if (verificaciones != null) {
            this.verificaciones.addAll(verificaciones);
        }
    }

    public void agregarVerificacion(Verificacion verificacion) {
        this.verificaciones.add(verificacion);
    }

    public List<Verificacion> getVerificaciones() {
        return List.copyOf(verificaciones);
    }

    @Override
    public ResultadoVerificacion verificar(ObjetivoMonitoreo objetivo) {
        if (verificaciones.isEmpty()) {
            return ResultadoVerificacion.builder()
                    .servicio(objetivo.getServicio())
                    .nombreMostrar(objetivo.getNombreMostrar())
                    .urlConsultada(objetivo.getUrlBase())
                    .estado(EstadoSalud.UP)
                    .latenciaMs(0L)
                    .ultimaVerificacion(LocalDateTime.now())
                    .tipoVerificacion(TipoVerificacion.COMPUESTA)
                    .detalle("Sin sub-verificaciones configuradas")
                    .build();
        }

        EstadoSalud peorEstado = EstadoSalud.UP;
        long latenciaTotal = 0L;
        Integer ultimoCodigoHttp = null;
        String urlPrincipal = objetivo.getUrlBase();
        List<String> detalles = new ArrayList<>();

        for (Verificacion subVerificacion : verificaciones) {
            ResultadoVerificacion parcial = subVerificacion.verificar(objetivo);
            latenciaTotal += parcial.getLatenciaMs();
            if (parcial.getCodigoHttp() != null) {
                ultimoCodigoHttp = parcial.getCodigoHttp();
            }
            if (parcial.getUrlConsultada() != null) {
                urlPrincipal = parcial.getUrlConsultada();
            }
            detalles.add(parcial.getDetalle());

            if (parcial.getEstado() == EstadoSalud.DOWN) {
                peorEstado = EstadoSalud.DOWN;
                // Reporta inmediatamente el primer chequeo crítico que falla en la cadena
                break;
            } else if (parcial.getEstado() == EstadoSalud.DEGRADADO && peorEstado == EstadoSalud.UP) {
                peorEstado = EstadoSalud.DEGRADADO;
            }
        }

        return ResultadoVerificacion.builder()
                .servicio(objetivo.getServicio())
                .nombreMostrar(objetivo.getNombreMostrar())
                .urlConsultada(urlPrincipal)
                .estado(peorEstado)
                .latenciaMs(latenciaTotal)
                .ultimaVerificacion(LocalDateTime.now())
                .codigoHttp(ultimoCodigoHttp)
                .tipoVerificacion(TipoVerificacion.COMPUESTA)
                .detalle(String.join(" | ", detalles))
                .build();
    }
}
