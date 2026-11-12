package br.unisinos.cvmsignals.retrieval.application;

import java.util.Optional;
import java.util.UUID;

/** Consulta com filtros combinados de metadados, conforme Seção 4.3. */
public record RetrievalRequest(
        String query,
        Optional<String> cnpj,
        Optional<String> documentType,
        Optional<UUID> documentId,
        int topK) {

    public static RetrievalRequest of(String query, int topK) {
        return new RetrievalRequest(query, Optional.empty(), Optional.empty(), Optional.empty(), topK);
    }

    public RetrievalRequest forDocument(UUID docId) {
        return new RetrievalRequest(query, cnpj, documentType, Optional.of(docId), topK);
    }
}
