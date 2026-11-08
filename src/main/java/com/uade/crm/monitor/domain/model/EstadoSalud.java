package com.uade.crm.monitor.domain.model;

/**
 * Estados posibles de salud de un microservicio monitoreado,
 * según el contrato en docs/contratos-backend.md.
 */
public enum EstadoSalud {
    UP,
    DEGRADADO,
    DOWN
}
