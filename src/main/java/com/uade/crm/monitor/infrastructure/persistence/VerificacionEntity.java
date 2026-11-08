package com.uade.crm.monitor.infrastructure.persistence;

import com.uade.crm.monitor.domain.model.EstadoSalud;
import com.uade.crm.monitor.domain.model.TipoVerificacion;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "verificaciones_salud", indexes = {
        @Index(name = "idx_verificacion_servicio_fecha", columnList = "servicio, ultimaVerificacion DESC")
})
public class VerificacionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String servicio;

    @Column(length = 100)
    private String nombreMostrar;

    @Column(length = 255)
    private String urlConsultada;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoSalud estado;

    @Column(nullable = false)
    private long latenciaMs;

    @Column(nullable = false)
    private LocalDateTime ultimaVerificacion;

    @Column(length = 500)
    private String detalle;

    private Integer codigoHttp;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private TipoVerificacion tipoVerificacion;
}
