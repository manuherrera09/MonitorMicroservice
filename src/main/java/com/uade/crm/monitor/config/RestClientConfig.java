package com.uade.crm.monitor.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor
public class RestClientConfig {

    private final MonitorProperties monitorProperties;

    @Bean
    public RestClient monitorRestClient() {
        long timeoutMs = monitorProperties.getTimeoutMs() > 0 ? monitorProperties.getTimeoutMs() : 3000L;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(timeoutMs));
        factory.setReadTimeout(Duration.ofMillis(timeoutMs));

        return RestClient.builder()
                .requestFactory(factory)
                .build();
    }
}
