package com.uade.crm.monitor.domain.port;

import com.uade.crm.monitor.domain.model.RespuestaSondeoHttp;

/**
 * Puerto de salida para realizar sondeos HTTP hacia los microservicios del ecosistema.
 */
public interface ClienteHttpSaludPort {

    RespuestaSondeoHttp sondearGet(String urlBase, String ruta);

    /**
     * Envía un sondeo HTTP con timeout extendido para despertar contenedores
     * hibernados (ej. cold start en Render/nube).
     */
    default RespuestaSondeoHttp sondearWakeUpGet(String urlBase, String ruta) {
        return sondearGet(urlBase, ruta);
    }
}
