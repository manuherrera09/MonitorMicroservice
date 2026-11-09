package com.uade.crm.monitor.infrastructure.client;

import com.uade.crm.monitor.domain.model.RespuestaSondeoHttp;
import com.uade.crm.monitor.domain.port.ClienteHttpSaludPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.concurrent.TimeUnit;

/**
 * Adaptador de infraestructura que realiza las peticiones HTTP de sondeo
 * hacia los microservicios y mide la latencia en milisegundos.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RestClientSaludAdapter implements ClienteHttpSaludPort {

    private final RestClient monitorRestClient;
    private final RestClient wakeUpRestClient;

    @Override
    public RespuestaSondeoHttp sondearGet(String urlBase, String ruta) {
        return ejecutarSondeo(monitorRestClient, urlBase, ruta);
    }

    @Override
    public RespuestaSondeoHttp sondearWakeUpGet(String urlBase, String ruta) {
        return ejecutarSondeo(wakeUpRestClient, urlBase, ruta);
    }

    private RespuestaSondeoHttp ejecutarSondeo(RestClient client, String urlBase, String ruta) {
        String urlCompleta = construirUrl(urlBase, ruta);
        long inicioNano = System.nanoTime();

        try {
            ResponseEntity<String> response = client.get()
                    .uri(urlCompleta)
                    .retrieve()
                    .toEntity(String.class);

            long latenciaMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicioNano);

            return RespuestaSondeoHttp.builder()
                    .alcanzable(true)
                    .codigoHttp(response.getStatusCode().value())
                    .latenciaMs(latenciaMs)
                    .cuerpo(response.getBody())
                    .urlConsultada(urlCompleta)
                    .build();

        } catch (RestClientResponseException ex) {
            long latenciaMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicioNano);
            return RespuestaSondeoHttp.builder()
                    .alcanzable(true)
                    .codigoHttp(ex.getStatusCode().value())
                    .latenciaMs(latenciaMs)
                    .cuerpo(ex.getResponseBodyAsString())
                    .urlConsultada(urlCompleta)
                    .mensajeError(ex.getStatusText())
                    .build();

        } catch (Exception ex) {
            long latenciaMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicioNano);
            String causa = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
            log.debug("Fallo al contactar {}: {}", urlCompleta, causa);

            return RespuestaSondeoHttp.builder()
                    .alcanzable(false)
                    .codigoHttp(0)
                    .latenciaMs(latenciaMs)
                    .urlConsultada(urlCompleta)
                    .mensajeError(causa != null ? causa : ex.getClass().getSimpleName())
                    .build();
        }
    }

    private String construirUrl(String urlBase, String ruta) {
        String base = urlBase != null ? urlBase.trim() : "";
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String path = (ruta != null && !ruta.isBlank()) ? ruta.trim() : "";
        if (!path.isEmpty() && !path.startsWith("/")) {
            path = "/" + path;
        }
        return base + path;
    }
}
