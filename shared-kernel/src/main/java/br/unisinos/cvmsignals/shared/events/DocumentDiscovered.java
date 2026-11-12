package br.unisinos.cvmsignals.shared.events;

import br.unisinos.cvmsignals.shared.DocumentType;

import java.time.LocalDate;
import java.util.UUID;

/** Publicado em cvm.docs.discovered após a descoberta de um documento na API da CVM. */
public record DocumentDiscovered(
        UUID documentId,
        String cnpj,
        String companyName,
        DocumentType type,
        LocalDate referenceDate,
        String downloadUrl) {
}
