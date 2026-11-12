package br.unisinos.cvmsignals.ingestion.application.port;

/** Converte o binário do documento (PDF, ZIP, HTML) em texto plano. */
public interface DocumentParserPort {

    String parse(byte[] content);
}
