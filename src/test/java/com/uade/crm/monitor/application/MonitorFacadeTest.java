package com.uade.crm.monitor.application;

import com.uade.crm.monitor.application.facade.MonitorFacade;
import com.uade.crm.monitor.config.MonitorProperties;
import com.uade.crm.monitor.domain.event.CambioEstadoServicioEvent;
import com.uade.crm.monitor.domain.model.EstadoSalud;
import com.uade.crm.monitor.domain.model.ResultadoVerificacion;
import com.uade.crm.monitor.domain.model.TipoVerificacion;
import com.uade.crm.monitor.domain.port.MonitorPersistencePort;
import com.uade.crm.monitor.domain.verificacion.Verificacion;
import com.uade.crm.monitor.domain.verificacion.VerificacionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MonitorFacadeTest {

    private MonitorProperties properties;
    private VerificacionFactory verificacionFactory;
    private MonitorPersistencePort persistencePort;
    private ApplicationEventPublisher eventPublisher;
    private MonitorFacade monitorFacade;

    @BeforeEach
    void setUp() {
        properties = new MonitorProperties();
        MonitorProperties.ServicioConfig srv = new MonitorProperties.ServicioConfig();
        srv.setServicio("calificacion");
        srv.setNombreMostrar("Microservicio de Calificación");
        srv.setUrlBase("http://localhost:8084");
        srv.setRutaSalud("/actuator/health");
        srv.setTipoVerificacion(TipoVerificacion.ACTUATOR_HEALTH);
        properties.setServicios(List.of(srv));

        verificacionFactory = mock(VerificacionFactory.class);
        persistencePort = mock(MonitorPersistencePort.class);
        eventPublisher = mock(ApplicationEventPublisher.class);

        monitorFacade = new MonitorFacade(properties, verificacionFactory, persistencePort, eventPublisher);
    }

    @Test
    @DisplayName("Facade + Observer: emite CambioEstadoServicioEvent cuando un servicio pasa de UP a DOWN")
    void emiteEventoCuandoCambiaDeEstado() {
        ResultadoVerificacion estadoPrevio = ResultadoVerificacion.builder()
                .id(1L)
                .servicio("calificacion")
                .estado(EstadoSalud.UP)
                .latenciaMs(40L)
                .ultimaVerificacion(LocalDateTime.now().minusSeconds(15))
                .detalle("Operativo")
                .build();

        ResultadoVerificacion nuevoResultado = ResultadoVerificacion.builder()
                .id(2L)
                .servicio("calificacion")
                .estado(EstadoSalud.DOWN)
                .latenciaMs(12L)
                .ultimaVerificacion(LocalDateTime.now())
                .detalle("Sin conexión: Connection refused")
                .build();

        when(persistencePort.buscarUltimaPorServicio("calificacion"))
                .thenReturn(Optional.of(estadoPrevio));

        Verificacion mockVerificacion = mock(Verificacion.class);
        when(mockVerificacion.verificar(any())).thenReturn(nuevoResultado);
        when(verificacionFactory.crearVerificacion(any())).thenReturn(mockVerificacion);
        when(persistencePort.guardar(any())).thenReturn(nuevoResultado);

        List<ResultadoVerificacion> resultados = monitorFacade.verificarTodos();

        assertThat(resultados).hasSize(1);
        assertThat(resultados.get(0).getEstado()).isEqualTo(EstadoSalud.DOWN);

        ArgumentCaptor<CambioEstadoServicioEvent> eventCaptor = ArgumentCaptor.forClass(CambioEstadoServicioEvent.class);
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());

        CambioEstadoServicioEvent emitido = eventCaptor.getValue();
        assertThat(emitido.getServicio()).isEqualTo("calificacion");
        assertThat(emitido.getEstadoAnterior()).isEqualTo(EstadoSalud.UP);
        assertThat(emitido.getEstadoNuevo()).isEqualTo(EstadoSalud.DOWN);
    }
}
