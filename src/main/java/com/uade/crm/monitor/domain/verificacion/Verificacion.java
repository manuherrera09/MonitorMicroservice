package com.uade.crm.monitor.domain.verificacion;

import com.uade.crm.monitor.domain.model.ObjetivoMonitoreo;
import com.uade.crm.monitor.domain.model.ResultadoVerificacion;

/**
 * Interfaz del Patrón Strategy para las distintas estrategias de verificación
 * de salud de un microservicio.
 */
public interface Verificacion {

    ResultadoVerificacion verificar(ObjetivoMonitoreo objetivo);
}
