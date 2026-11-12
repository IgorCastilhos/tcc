package br.unisinos.cvmsignals.indexing.infrastructure;

import br.unisinos.cvmsignals.indexing.application.port.IndexingEventPublisherPort;
import br.unisinos.cvmsignals.shared.Topics;
import br.unisinos.cvmsignals.shared.events.ChunksEmbedded;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaIndexingEventPublisher implements IndexingEventPublisherPort {

    private final KafkaTemplate<String, Object> kafka;

    public KafkaIndexingEventPublisher(KafkaTemplate<String, Object> kafka) {
        this.kafka = kafka;
    }

    @Override
    public void publish(ChunksEmbedded event) {
        kafka.send(Topics.CHUNKS_EMBEDDED, event.documentId().toString(), event);
    }
}
