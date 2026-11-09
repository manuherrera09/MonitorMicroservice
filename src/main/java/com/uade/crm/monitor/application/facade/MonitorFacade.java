package com.uade.crm.monitor.application.facade;

import com.uade.crm.monitor.config.MonitorProperties;
import com.uade.crm.monitor.domain.event.CambioEstadoServicioEvent;
import com.uade.crm.monitor.domain.model.EstadoSalud;
import com.uade.crm.monitor.domain.model.ObjetivoMonitoreo;
import com.uade.crm.monitor.domain.model.ResultadoVerificacion;
import com.uade.crm.monitor.domain.port.ClienteHttpSaludPort;
import com.uade.crm.monitor.domain.port.MonitorPersistencePort;
import com.uade.crm.monitor.domain.verificacion.Verificacion;
import com.uade.crm.monitor.domain.verificacion.VerificacionFactory;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Implementación del Patrón Facade:
 * Coordina la ejecución de verificaciones sobre los microservicios,
 * el envío de pings de reactivación (wake-up para instancias hibernadas),
 * la persistencia del estado/historial y la emisión de eventos de cambio de estado (Observer).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MonitorFacade {

    private final MonitorProperties monitorProperties;
    private final VerificacionFactory verificacionFactory;
    private final MonitorPersistencePort persistencePort;
    private final ClienteHttpSaludPort clienteHttpSaludPort;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Ejecuta la verificación en paralelo de todos los servicios configurados y retorna su estado actualizado.
     */
    public List<ResultadoVerificacion> verificarTodos() {
        List<ObjetivoMonitoreo> objetivos = monitorProperties.toObjetivosMonitoreo();

        List<CompletableFuture<ResultadoVerificacion>> futuros = objetivos.stream()
                .map(objetivo -> CompletableFuture.supplyAsync(() -> verificarObjetivo(objetivo)))
                .toList();

        return futuros.stream()
                .map(CompletableFuture::join)
                .toList();
    }

    /**
     * Ejecuta la verificación inmediata de un servicio específico por nombre.
     */
    public ResultadoVerificacion verificarServicio(String nombreServicio) {
        ObjetivoMonitoreo objetivo = buscarObjetivoPorNombre(nombreServicio);
        return verificarObjetivo(objetivo);
    }

    /**
     * Envía señales de reactivación (wake-up ping con reintentos y timeout extendido) en paralelo
     * a todos los microservicios (o a uno específico) para levantar instancias inactivas/hibernadas.
     */
    public List<ResultadoVerificacion> despertarServicios(String nombreServicio) {
        List<ObjetivoMonitoreo> objetivos;
        if (nombreServicio != null && !nombreServicio.isBlank()) {
            objetivos = List.of(buscarObjetivoPorNombre(nombreServicio.trim()));
        } else {
            objetivos = monitorProperties.toObjetivosMonitoreo();
        }

        List<CompletableFuture<ResultadoVerificacion>> futuros = objetivos.stream()
                .map(objetivo -> CompletableFuture.supplyAsync(() -> despertarObjetivo(objetivo)))
                .toList();

        return futuros.stream()
                .map(CompletableFuture::join)
                .toList();
    }

    /**
     * Obtiene el último estado registrado de cada servicio monitoreado.
     * Si algún servicio aún no tiene verificaciones previas, lo verifica en el momento.
     */
    public List<ResultadoVerificacion> obtenerEstadoActual() {
        List<ObjetivoMonitoreo> objetivos = monitorProperties.toObjetivosMonitoreo();
        List<ResultadoVerificacion> estados = new ArrayList<>();

        for (ObjetivoMonitoreo objetivo : objetivos) {
            Optional<ResultadoVerificacion> ultima = persistencePort.buscarUltimaPorServicio(objetivo.getServicio());
            if (ultima.isPresent()) {
                estados.add(ultima.get());
            } else {
                estados.add(verificarObjetivo(objetivo));
            }
        }

        return estados;
    }

    /**
     * Obtiene el historial de verificaciones. Si se indica `servicio`, filtra por ese microservicio.
     */
    public List<ResultadoVerificacion> obtenerHistorial(String servicio, Integer limite) {
        int maxResultados = (limite != null && limite > 0)
                ? Math.min(limite, 200)
                : monitorProperties.getLimiteHistorialPorDefecto();

        if (servicio != null && !servicio.isBlank()) {
            return persistencePort.buscarHistorialPorServicio(servicio.trim().toLowerCase(), maxResultados);
        }
        return persistencePort.buscarHistorialGeneral(maxResultados);
    }

    private ObjetivoMonitoreo buscarObjetivoPorNombre(String nombreServicio) {
        return monitorProperties.toObjetivosMonitoreo().stream()
                .filter(o -> o.getServicio().equalsIgnoreCase(nombreServicio))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Servicio no registrado en el monitor: " + nombreServicio));
    }

    private ResultadoVerificacion despertarObjetivo(ObjetivoMonitoreo objetivo) {
        int maxIntentos = Math.max(1, monitorProperties.getDespertarReintentos());
        long esperaMs = Math.max(0L, monitorProperties.getDespertarEsperaEntreIntentosMs());
        String rutaPrimaria = objetivo.getRutaSalud() != null && !objetivo.getRutaSalud().isBlank()
                ? objetivo.getRutaSalud()
                : "/actuator/health";

        ResultadoVerificacion ultimoResultado = null;

        for (int intento = 1; intento <= maxIntentos; intento++) {
            log.info("[WAKE-UP] Enviando ping de activación a '{}' (intento {}/{})",
                    objetivo.getServicio(), intento, maxIntentos);

            // Dispara petición con timeout extendido para despertar el contenedor si estaba hibernado
            clienteHttpSaludPort.sondearWakeUpGet(objetivo.getUrlBase(), rutaPrimaria);

            ultimoResultado = verificarObjetivo(objetivo);
            if (ultimoResultado.getEstado() == EstadoSalud.UP) {
                break;
            }

            if (intento < maxIntentos && esperaMs > 0) {
                try {
                    Thread.sleep(esperaMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        return ultimoResultado != null ? ultimoResultado : verificarObjetivo(objetivo);
    }

    private ResultadoVerificacion verificarObjetivo(ObjetivoMonitoreo objetivo) {
        Optional<ResultadoVerificacion> estadoPrevioOpt = persistencePort.buscarUltimaPorServicio(objetivo.getServicio());
        EstadoSalud estadoAnterior = estadoPrevioOpt.map(ResultadoVerificacion::getEstado).orElse(null);

        Verificacion estrategia = verificacionFactory.crearVerificacion(objetivo);
        ResultadoVerificacion nuevoResultado = estrategia.verificar(objetivo);

        ResultadoVerificacion guardado = persistencePort.guardar(nuevoResultado);

        // Si hubo transición de estado (ej. UP -> DOWN o DOWN -> UP), notificamos a los Observers
        if (estadoAnterior != null && estadoAnterior != guardado.getEstado()) {
            CambioEstadoServicioEvent evento = CambioEstadoServicioEvent.builder()
                    .servicio(guardado.getServicio())
                    .estadoAnterior(estadoAnterior)
                    .estadoNuevo(guardado.getEstado())
                    .latenciaMs(guardado.getLatenciaMs())
                    .detalle(guardado.getDetalle())
                    .fechaCambio(LocalDateTime.now())
                    .verificacion(guardado)
                    .build();
            eventPublisher.publishEvent(evento);
        }

        return guardado;
    }
}
