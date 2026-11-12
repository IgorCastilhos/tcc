package br.unisinos.cvmsignals.ingestion.domain;

/** Ciclo de vida de um documento dentro do pipeline. */
public enum DocumentStatus {
    DISCOVERED,
    PARSED,
    FAILED
}
