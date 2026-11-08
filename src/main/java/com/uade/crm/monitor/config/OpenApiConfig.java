package com.uade.crm.monitor.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Microservicio Monitor (Health Check) - CRM Automotriz")
                        .description("API REST para el monitoreo de salud, latencia e historial de los microservicios del ecosistema DDA Motors. UADE - Desarrollo de Aplicaciones II.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Equipo de Desarrollo UADE")
                                .email("contacto@uade.edu.ar")));
    }
}
