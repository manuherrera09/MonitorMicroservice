package com.uade.crm.monitor.domain.port;

import com.uade.crm.monitor.domain.model.RespuestaSondeoHttp;

/**
 * Puerto de salida para realizar sondeos HTTP hacia los microservicios del ecosistema.
 */
public interface ClienteHttpSaludPort {

    RespuestaSondeoHttp sondearGet(String urlBase, String ruta);
}
