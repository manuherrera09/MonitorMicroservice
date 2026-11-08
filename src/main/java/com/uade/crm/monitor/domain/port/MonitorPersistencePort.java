package com.uade.crm.monitor.domain.port;

import com.uade.crm.monitor.domain.model.ResultadoVerificacion;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida para la persistencia del estado actual e historial de verificaciones.
 */
public interface MonitorPersistencePort {

    ResultadoVerificacion guardar(ResultadoVerificacion resultado);

    Optional<ResultadoVerificacion> buscarUltimaPorServicio(String servicio);

    List<ResultadoVerificacion> buscarHistorialPorServicio(String servicio, int limite);

    List<ResultadoVerificacion> buscarHistorialGeneral(int limite);
}
