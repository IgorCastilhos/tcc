package br.unisinos.cvmsignals.signalextraction.application.port;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Recupera os trechos mais relevantes do documento e seus metadados de exibição. */
public interface DocumentContextPort {

    Optional<DocumentInfo> documentInfo(UUID documentId);

    List<String> topChunks(UUID documentId, String query, int topK);

    record DocumentInfo(UUID id, String cnpj, String companyName, String type) {
    }
}
