package br.unisinos.cvmsignals.ingestion.application;

import br.unisinos.cvmsignals.shared.DocumentType;

import java.time.LocalDate;

/** Metadados de um documento anunciado pela CVM, antes de existir como agregado. */
public record DiscoveredDocument(
        String cnpj,
        String companyName,
        String cvmCode,
        DocumentType type,
        LocalDate referenceDate,
        String downloadUrl,
        String protocol) {
}
