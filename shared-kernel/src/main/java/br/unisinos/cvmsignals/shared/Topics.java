package br.unisinos.cvmsignals.shared;

/**
 * Nomes dos tópicos Kafka que formam o backbone de eventos do pipeline
 * (Seção 4.2 da monografia). Centralizados aqui para manter a linguagem
 * ubíqua consistente entre os bounded contexts.
 */
public final class Topics {

    public static final String DOCS_DISCOVERED = "cvm.docs.discovered";
    public static final String DOCS_PARSED = "cvm.docs.parsed";
    public static final String CHUNKS_EMBEDDED = "cvm.chunks.embedded";
    public static final String SIGNALS_EXTRACTED = "cvm.signals.extracted";

    private Topics() {
    }
}
