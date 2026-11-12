package br.unisinos.cvmsignals.ingestion.infrastructure.messaging;

import br.unisinos.cvmsignals.ingestion.application.port.IngestionEventPublisherPort;
import br.unisinos.cvmsignals.shared.Topics;
import br.unisinos.cvmsignals.shared.events.DocumentDiscovered;
import br.unisinos.cvmsignals.shared.events.DocumentParsed;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaIngestionEventPublisher implements IngestionEventPublisherPort {

    private final KafkaTemplate<String, Object> kafka;

    public KafkaIngestionEventPublisher(KafkaTemplate<String, Object> kafka) {
        this.kafka = kafka;
    }

    @Override
    public void publish(DocumentDiscovered event) {
        kafka.send(Topics.DOCS_DISCOVERED, event.documentId().toString(), event);
    }

    @Override
    public void publish(DocumentParsed event) {
        kafka.send(Topics.DOCS_PARSED, event.documentId().toString(), event);
    }
}
