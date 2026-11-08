package com.uade.crm.monitor.domain;

import com.uade.crm.monitor.domain.model.EstadoSalud;
import com.uade.crm.monitor.domain.model.ObjetivoMonitoreo;
import com.uade.crm.monitor.domain.model.RespuestaSondeoHttp;
import com.uade.crm.monitor.domain.model.ResultadoVerificacion;
import com.uade.crm.monitor.domain.model.TipoVerificacion;
import com.uade.crm.monitor.domain.port.ClienteHttpSaludPort;
import com.uade.crm.monitor.domain.verificacion.Verificacion;
import com.uade.crm.monitor.domain.verificacion.VerificacionCompuesta;
import com.uade.crm.monitor.domain.verificacion.VerificacionDisponibilidadHttp;
import com.uade.crm.monitor.domain.verificacion.VerificacionFactory;
import com.uade.crm.monitor.domain.verificacion.VerificacionSalud;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class VerificacionPatronesTest {

    private ObjetivoMonitoreo crearObjetivoBase() {
        return ObjetivoMonitoreo.builder()
                .servicio("vehiculos")
                .nombreMostrar("Microservicio de Vehículos")
                .urlBase("http://localhost:8083")
                .rutaSalud("/actuator/health")
                .rutaAlternativa("/v3/api-docs")
                .tipoVerificacion(TipoVerificacion.ACTUATOR_HEALTH)
                .umbralDegradadoMs(1000L)
                .build();
    }

    @Test
    @DisplayName("Strategy: VerificacionSalud retorna UP cuando Actuator responde 200 OK dentro del umbral")
    void verificacionSaludUp() {
        ClienteHttpSaludPort mockPort = mock(ClienteHttpSaludPort.class);
        when(mockPort.sondearGet("http://localhost:8083", "/actuator/health"))
                .thenReturn(RespuestaSondeoHttp.builder()
                        .alcanzable(true)
                        .codigoHttp(200)
                        .latenciaMs(45L)
                        .cuerpo("{\"status\":\"UP\"}")
                        .urlConsultada("http://localhost:8083/actuator/health")
                        .build());

        Verificacion estrategia = new VerificacionSalud(mockPort);
        ResultadoVerificacion resultado = estrategia.verificar(crearObjetivoBase());

        assertThat(resultado.getEstado()).isEqualTo(EstadoSalud.UP);
        assertThat(resultado.getLatenciaMs()).isEqualTo(45L);
        assertThat(resultado.getCodigoHttp()).isEqualTo(200);
    }

    @Test
    @DisplayName("Strategy: VerificacionSalud retorna DEGRADADO cuando la latencia supera el umbral")
    void verificacionSaludDegradadoPorLatencia() {
        ClienteHttpSaludPort mockPort = mock(ClienteHttpSaludPort.class);
        when(mockPort.sondearGet("http://localhost:8083", "/actuator/health"))
                .thenReturn(RespuestaSondeoHttp.builder()
                        .alcanzable(true)
                        .codigoHttp(200)
                        .latenciaMs(1450L)
                        .cuerpo("{\"status\":\"UP\"}")
                        .urlConsultada("http://localhost:8083/actuator/health")
                        .build());

        Verificacion estrategia = new VerificacionSalud(mockPort);
        ResultadoVerificacion resultado = estrategia.verificar(crearObjetivoBase());

        assertThat(resultado.getEstado()).isEqualTo(EstadoSalud.DEGRADADO);
        assertThat(resultado.getLatenciaMs()).isEqualTo(1450L);
    }

    @Test
    @DisplayName("Strategy: VerificacionSalud usa ruta alternativa cuando /actuator/health devuelve 404")
    void verificacionSaludFallbackRutaAlternativa() {
        ClienteHttpSaludPort mockPort = mock(ClienteHttpSaludPort.class);
        when(mockPort.sondearGet("http://localhost:8083", "/actuator/health"))
                .thenReturn(RespuestaSondeoHttp.builder()
                        .alcanzable(true)
                        .codigoHttp(404)
                        .latenciaMs(20L)
                        .urlConsultada("http://localhost:8083/actuator/health")
                        .build());
        when(mockPort.sondearGet("http://localhost:8083", "/v3/api-docs"))
                .thenReturn(RespuestaSondeoHttp.builder()
                        .alcanzable(true)
                        .codigoHttp(200)
                        .latenciaMs(35L)
                        .cuerpo("{\"openapi\":\"3.0.1\"}")
                        .urlConsultada("http://localhost:8083/v3/api-docs")
                        .build());

        Verificacion estrategia = new VerificacionSalud(mockPort);
        ResultadoVerificacion resultado = estrategia.verificar(crearObjetivoBase());

        assertThat(resultado.getEstado()).isEqualTo(EstadoSalud.UP);
        assertThat(resultado.getUrlConsultada()).isEqualTo("http://localhost:8083/v3/api-docs");
    }

    @Test
    @DisplayName("Strategy: VerificacionSalud retorna DOWN cuando el servicio no es alcanzable")
    void verificacionSaludDownPorConexion() {
        ClienteHttpSaludPort mockPort = mock(ClienteHttpSaludPort.class);
        when(mockPort.sondearGet("http://localhost:8083", "/actuator/health"))
                .thenReturn(RespuestaSondeoHttp.builder()
                        .alcanzable(false)
                        .codigoHttp(0)
                        .latenciaMs(15L)
                        .mensajeError("Connection refused")
                        .urlConsultada("http://localhost:8083/actuator/health")
                        .build());

        Verificacion estrategia = new VerificacionSalud(mockPort);
        ResultadoVerificacion resultado = estrategia.verificar(crearObjetivoBase());

        assertThat(resultado.getEstado()).isEqualTo(EstadoSalud.DOWN);
        assertThat(resultado.getDetalle()).contains("Connection refused");
    }

    @Test
    @DisplayName("Composite: VerificacionCompuesta consolida múltiples verificaciones y corta ante un fallo DOWN")
    void verificacionCompuestaReportaFallo() {
        ClienteHttpSaludPort mockPort = mock(ClienteHttpSaludPort.class);
        when(mockPort.sondearGet("http://localhost:8083", "/actuator/health"))
                .thenReturn(RespuestaSondeoHttp.builder()
                        .alcanzable(true)
                        .codigoHttp(200)
                        .latenciaMs(30L)
                        .cuerpo("{\"status\":\"UP\"}")
                        .urlConsultada("http://localhost:8083/actuator/health")
                        .build());
        when(mockPort.sondearGet("http://localhost:8083", "/v3/api-docs"))
                .thenReturn(RespuestaSondeoHttp.builder()
                        .alcanzable(true)
                        .codigoHttp(503)
                        .latenciaMs(50L)
                        .urlConsultada("http://localhost:8083/v3/api-docs")
                        .build());

        VerificacionCompuesta compuesta = new VerificacionCompuesta(List.of(
                new VerificacionSalud(mockPort),
                new VerificacionDisponibilidadHttp(mockPort)
        ));

        ResultadoVerificacion resultado = compuesta.verificar(crearObjetivoBase());

        assertThat(resultado.getEstado()).isEqualTo(EstadoSalud.DOWN);
        assertThat(resultado.getLatenciaMs()).isEqualTo(80L);
        assertThat(resultado.getTipoVerificacion()).isEqualTo(TipoVerificacion.COMPUESTA);
    }

    @Test
    @DisplayName("Factory: VerificacionFactory instancia la estrategia correcta según TipoVerificacion")
    void factoryCreaEstrategiaCorrecta() {
        ClienteHttpSaludPort mockPort = mock(ClienteHttpSaludPort.class);
        VerificacionFactory factory = new VerificacionFactory(mockPort);

        ObjetivoMonitoreo objCompuesto = crearObjetivoBase();
        objCompuesto.setTipoVerificacion(TipoVerificacion.COMPUESTA);

        Verificacion verificacion = factory.crearVerificacion(objCompuesto);
        assertThat(verificacion).isInstanceOf(VerificacionCompuesta.class);
    }
}
