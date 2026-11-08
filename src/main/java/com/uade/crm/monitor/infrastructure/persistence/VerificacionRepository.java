package com.uade.crm.monitor.infrastructure.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VerificacionRepository extends JpaRepository<VerificacionEntity, Long> {

    Optional<VerificacionEntity> findTopByServicioIgnoreCaseOrderByUltimaVerificacionDesc(String servicio);

    List<VerificacionEntity> findByServicioIgnoreCaseOrderByUltimaVerificacionDesc(String servicio, Pageable pageable);

    List<VerificacionEntity> findAllByOrderByUltimaVerificacionDesc(Pageable pageable);
}
