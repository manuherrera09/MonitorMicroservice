package com.uade.crm.monitor.application.listener;

import com.uade.crm.monitor.domain.event.CambioEstadoServicioEvent;
import com.uade.crm.monitor.domain.model.EstadoSalud;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Implementación del Patrón Observer (según docs/contratos-backend.md):
 * Reacciona de forma desacoplada cuando un microservicio cambia de estado
 * (por ejemplo de UP a DOWN o cuando se recupera).
 */
@Slf4j
@Getter
@Component
public class CambioEstadoServicioListener {

    private static final int MAX_EVENTOS_MEMORIA = 100;
    private final List<CambioEstadoServicioEvent> bitacoraCambios = new CopyOnWriteArrayList<>();

    @EventListener
    public void onCambioEstadoServicio(CambioEstadoServicioEvent event) {
        if (event.getEstadoNuevo() == EstadoSalud.DOWN) {
            log.error("[ALERTA MONITOR] El servicio '{}' cayó: {} -> {} ({} ms) - {}",
                    event.getServicio(),
                    event.getEstadoAnterior(),
                    event.getEstadoNuevo(),
                    event.getLatenciaMs(),
                    event.getDetalle());
        } else if (event.getEstadoNuevo() == EstadoSalud.DEGRADADO) {
            log.warn("[AVISO MONITOR] El servicio '{}' se encuentra DEGRADADO: {} -> {} ({} ms) - {}",
                    event.getServicio(),
                    event.getEstadoAnterior(),
                    event.getEstadoNuevo(),
                    event.getLatenciaMs(),
                    event.getDetalle());
        } else {
            log.info("[RECUPERACIÓN MONITOR] El servicio '{}' volvió a estar operativo: {} -> {} ({} ms)",
                    event.getServicio(),
                    event.getEstadoAnterior(),
                    event.getEstadoNuevo(),
                    event.getLatenciaMs());
        }

        bitacoraCambios.add(0, event);
        while (bitacoraCambios.size() > MAX_EVENTOS_MEMORIA) {
            bitacoraCambios.remove(bitacoraCambios.size() - 1);
        }
    }
}
