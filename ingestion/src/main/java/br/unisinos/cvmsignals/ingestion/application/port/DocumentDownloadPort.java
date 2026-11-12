package br.unisinos.cvmsignals.ingestion.application.port;

/** Baixa o conteúdo binário de um documento a partir da URL divulgada pela CVM. */
public interface DocumentDownloadPort {

    byte[] download(String url);
}
