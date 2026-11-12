package br.unisinos.cvmsignals.signalextraction.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataSignals extends JpaRepository<SignalEntity, UUID> {

    List<SignalEntity> findByDocumentoId(UUID documentoId);
}
