package br.unisinos.cvmsignals.shared.events;

import br.unisinos.cvmsignals.shared.DocumentType;

import java.util.UUID;

/** Publicado em cvm.docs.parsed quando o texto do documento foi extraído e persistido. */
public record DocumentParsed(UUID documentId, String cnpj, DocumentType type, int textLength) {
}
