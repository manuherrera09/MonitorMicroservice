package com.uade.crm.monitor.config;

import com.uade.crm.monitor.domain.model.ObjetivoMonitoreo;
import com.uade.crm.monitor.domain.model.TipoVerificacion;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "crm.monitor")
public class MonitorProperties {

    private boolean planificadorActivo = true;
    private long intervaloMs = 15000L;
    private long timeoutMs = 3000L;
    private long umbralDegradadoMs = 1000L;
    private int limiteHistorialPorDefecto = 50;
    private List<ServicioConfig> servicios = new ArrayList<>();

    @Data
    public static class ServicioConfig {
        private String servicio;
        private String nombreMostrar;
        private String urlBase;
        private String rutaSalud = "/actuator/health";
        private String rutaAlternativa = "/v3/api-docs";
        private TipoVerificacion tipoVerificacion = TipoVerificacion.ACTUATOR_HEALTH;
        private Long umbralDegradadoMs;
    }

    public List<ObjetivoMonitoreo> toObjetivosMonitoreo() {
        return servicios.stream()
                .map(s -> ObjetivoMonitoreo.builder()
                        .servicio(s.getServicio())
                        .nombreMostrar(s.getNombreMostrar() != null ? s.getNombreMostrar() : s.getServicio())
                        .urlBase(s.getUrlBase())
                        .rutaSalud(s.getRutaSalud())
                        .rutaAlternativa(s.getRutaAlternativa())
                        .tipoVerificacion(s.getTipoVerificacion() != null ? s.getTipoVerificacion() : TipoVerificacion.ACTUATOR_HEALTH)
                        .umbralDegradadoMs(s.getUmbralDegradadoMs() != null ? s.getUmbralDegradadoMs() : umbralDegradadoMs)
                        .build())
                .toList();
    }
}
