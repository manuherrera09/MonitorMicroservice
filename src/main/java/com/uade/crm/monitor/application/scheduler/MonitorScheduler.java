package com.uade.crm.monitor.application.scheduler;

import com.uade.crm.monitor.application.facade.MonitorFacade;
import com.uade.crm.monitor.config.MonitorProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Planificador periódico que ejecuta el chequeo automático de todos los microservicios.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MonitorScheduler {

    private final MonitorFacade monitorFacade;
    private final MonitorProperties monitorProperties;

    @EventListener(ApplicationReadyEvent.class)
    public void verificarAlIniciar() {
        if (!monitorProperties.isPlanificadorActivo()) {
            return;
        }
        log.info("Ejecutando verificación inicial de salud de los microservicios...");
        monitorFacade.verificarTodos();
    }

    @Scheduled(fixedDelayString = "${crm.monitor.intervalo-ms:15000}", initialDelayString = "${crm.monitor.intervalo-ms:15000}")
    public void verificarPeriodicamente() {
        if (!monitorProperties.isPlanificadorActivo()) {
            return;
        }
        log.debug("Ejecutando ronda periódica de monitoreo...");
        monitorFacade.verificarTodos();
    }
}
