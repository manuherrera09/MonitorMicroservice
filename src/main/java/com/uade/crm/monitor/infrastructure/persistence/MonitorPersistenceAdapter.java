package com.uade.crm.monitor.infrastructure.persistence;

import com.uade.crm.monitor.domain.model.ResultadoVerificacion;
import com.uade.crm.monitor.domain.port.MonitorPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Implementación del Patrón Repository / Adapter de persistencia:
 * Aísla el modelo de dominio del almacenamiento relacional JPA.
 */
@Component
@RequiredArgsConstructor
public class MonitorPersistenceAdapter implements MonitorPersistencePort {

    private final VerificacionRepository repository;

    @Override
    public ResultadoVerificacion guardar(ResultadoVerificacion resultado) {
        VerificacionEntity entity = toEntity(resultado);
        VerificacionEntity guardada = repository.save(entity);
        return toDomain(guardada);
    }

    @Override
    public Optional<ResultadoVerificacion> buscarUltimaPorServicio(String servicio) {
        return repository.findTopByServicioIgnoreCaseOrderByUltimaVerificacionDesc(servicio)
                .map(this::toDomain);
    }

    @Override
    public List<ResultadoVerificacion> buscarHistorialPorServicio(String servicio, int limite) {
        return repository.findByServicioIgnoreCaseOrderByUltimaVerificacionDesc(servicio, PageRequest.of(0, limite))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<ResultadoVerificacion> buscarHistorialGeneral(int limite) {
        return repository.findAllByOrderByUltimaVerificacionDesc(PageRequest.of(0, limite))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private VerificacionEntity toEntity(ResultadoVerificacion d) {
        return VerificacionEntity.builder()
                .id(d.getId())
                .servicio(d.getServicio())
                .nombreMostrar(d.getNombreMostrar())
                .urlConsultada(d.getUrlConsultada())
                .estado(d.getEstado())
                .latenciaMs(d.getLatenciaMs())
                .ultimaVerificacion(d.getUltimaVerificacion())
                .detalle(d.getDetalle())
                .codigoHttp(d.getCodigoHttp())
                .tipoVerificacion(d.getTipoVerificacion())
                .build();
    }

    private ResultadoVerificacion toDomain(VerificacionEntity e) {
        return ResultadoVerificacion.builder()
                .id(e.getId())
                .servicio(e.getServicio())
                .nombreMostrar(e.getNombreMostrar())
                .urlConsultada(e.getUrlConsultada())
                .estado(e.getEstado())
                .latenciaMs(e.getLatenciaMs())
                .ultimaVerificacion(e.getUltimaVerificacion())
                .detalle(e.getDetalle())
                .codigoHttp(e.getCodigoHttp())
                .tipoVerificacion(e.getTipoVerificacion())
                .build();
    }
}
