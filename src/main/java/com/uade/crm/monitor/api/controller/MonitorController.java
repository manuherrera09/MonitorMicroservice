package com.uade.crm.monitor.api.controller;

import com.uade.crm.monitor.api.dto.EstadoServicioResponse;
import com.uade.crm.monitor.api.dto.ResumenMonitorResponse;
import com.uade.crm.monitor.application.facade.MonitorFacade;
import com.uade.crm.monitor.domain.model.EstadoSalud;
import com.uade.crm.monitor.domain.model.ResultadoVerificacion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/monitor")
@RequiredArgsConstructor
@Tag(name = "Monitor", description = "Endpoints para consulta de salud, latencia e historial de los microservicios")
public class MonitorController {

    private final MonitorFacade monitorFacade;

    @Operation(
            summary = "Obtener estado actual de los microservicios",
            description = "Retorna la lista de microservicios monitoreados con su estado (UP, DEGRADADO, DOWN), latencia en ms, fecha de última verificación y detalle."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estados obtenidos exitosamente")
    })
    @GetMapping("/estado")
    public ResponseEntity<List<EstadoServicioResponse>> obtenerEstado(
            @Parameter(description = "Si es true, fuerza un sondeo en vivo antes de responder", example = "false")
            @RequestParam(name = "forzar", defaultValue = "false") boolean forzar) {

        List<ResultadoVerificacion> resultados = forzar
                ? monitorFacade.verificarTodos()
                : monitorFacade.obtenerEstadoActual();

        List<EstadoServicioResponse> response = resultados.stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Obtener historial de verificaciones",
            description = "Lista las últimas verificaciones registradas en orden cronológico inverso. Permite filtrar por nombre de servicio."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historial obtenido exitosamente")
    })
    @GetMapping("/historial")
    public ResponseEntity<List<EstadoServicioResponse>> obtenerHistorial(
            @Parameter(description = "Nombre del microservicio a filtrar (opcional: vehiculos, clientes, leads, calificacion)", example = "vehiculos")
            @RequestParam(name = "servicio", required = false) String servicio,
            @Parameter(description = "Cantidad máxima de registros a devolver (por defecto 50)", example = "50")
            @RequestParam(name = "limite", required = false) Integer limite) {

        List<ResultadoVerificacion> historial = monitorFacade.obtenerHistorial(servicio, limite);
        List<EstadoServicioResponse> response = historial.stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Ejecutar verificación inmediata de salud",
            description = "Fuerza un chequeo inmediato de todos los microservicios (o de uno en particular si se indica el parámetro `servicio`)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Verificación ejecutada exitosamente")
    })
    @PostMapping("/verificar")
    public ResponseEntity<List<EstadoServicioResponse>> ejecutarVerificacionInmediata(
            @Parameter(description = "Nombre del servicio específico a verificar (opcional)", example = "calificacion")
            @RequestParam(name = "servicio", required = false) String servicio) {

        List<ResultadoVerificacion> resultados;
        if (servicio != null && !servicio.isBlank()) {
            resultados = List.of(monitorFacade.verificarServicio(servicio.trim()));
        } else {
            resultados = monitorFacade.verificarTodos();
        }

        List<EstadoServicioResponse> response = resultados.stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Obtener resumen general del ecosistema",
            description = "Retorna métricas agregadas del sistema (estado global, cantidad de servicios UP/DEGRADADO/DOWN y latencia promedio)."
    )
    @GetMapping("/resumen")
    public ResponseEntity<ResumenMonitorResponse> obtenerResumen() {
        List<EstadoServicioResponse> servicios = monitorFacade.obtenerEstadoActual().stream()
                .map(this::toResponse)
                .toList();

        int up = (int) servicios.stream().filter(s -> s.getEstado() == EstadoSalud.UP).count();
        int degradados = (int) servicios.stream().filter(s -> s.getEstado() == EstadoSalud.DEGRADADO).count();
        int down = (int) servicios.stream().filter(s -> s.getEstado() == EstadoSalud.DOWN).count();

        EstadoSalud estadoGlobal = EstadoSalud.UP;
        if (down > 0) {
            estadoGlobal = down == servicios.size() ? EstadoSalud.DOWN : EstadoSalud.DEGRADADO;
        } else if (degradados > 0) {
            estadoGlobal = EstadoSalud.DEGRADADO;
        }

        long latenciaPromedio = servicios.isEmpty() ? 0L : Math.round(
                servicios.stream().mapToLong(EstadoServicioResponse::getLatenciaMs).average().orElse(0.0)
        );

        LocalDateTime ultimaVerif = servicios.stream()
                .map(EstadoServicioResponse::getUltimaVerificacion)
                .filter(f -> f != null)
                .max(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());

        ResumenMonitorResponse resumen = ResumenMonitorResponse.builder()
                .estadoGlobal(estadoGlobal)
                .totalServicios(servicios.size())
                .serviciosUp(up)
                .serviciosDegradados(degradados)
                .serviciosDown(down)
                .latenciaPromedioMs(latenciaPromedio)
                .ultimaVerificacion(ultimaVerif)
                .servicios(servicios)
                .build();

        return ResponseEntity.ok(resumen);
    }

    private EstadoServicioResponse toResponse(ResultadoVerificacion d) {
        return EstadoServicioResponse.builder()
                .id(d.getId())
                .servicio(d.getServicio())
                .nombreMostrar(d.getNombreMostrar())
                .estado(d.getEstado())
                .latenciaMs(d.getLatenciaMs())
                .ultimaVerificacion(d.getUltimaVerificacion())
                .detalle(d.getDetalle())
                .urlConsultada(d.getUrlConsultada())
                .codigoHttp(d.getCodigoHttp())
                .tipoVerificacion(d.getTipoVerificacion())
                .build();
    }
}
