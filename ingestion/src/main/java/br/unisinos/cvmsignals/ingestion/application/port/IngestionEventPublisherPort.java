package br.unisinos.cvmsignals.ingestion.application.port;

import br.unisinos.cvmsignals.shared.events.DocumentDiscovered;
import br.unisinos.cvmsignals.shared.events.DocumentParsed;

/** Porta de saída para o backbone de eventos (Kafka). */
public interface IngestionEventPublisherPort {

    void publish(DocumentDiscovered event);

    void publish(DocumentParsed event);
}
