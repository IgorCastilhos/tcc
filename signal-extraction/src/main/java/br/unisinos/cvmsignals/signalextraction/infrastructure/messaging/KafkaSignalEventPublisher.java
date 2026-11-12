package br.unisinos.cvmsignals.signalextraction.infrastructure.messaging;

import br.unisinos.cvmsignals.signalextraction.application.port.SignalEventPublisherPort;
import br.unisinos.cvmsignals.shared.Topics;
import br.unisinos.cvmsignals.shared.events.SignalsExtracted;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaSignalEventPublisher implements SignalEventPublisherPort {

    private final KafkaTemplate<String, Object> kafka;

    public KafkaSignalEventPublisher(KafkaTemplate<String, Object> kafka) {
        this.kafka = kafka;
    }

    @Override
    public void publish(SignalsExtracted event) {
        kafka.send(Topics.SIGNALS_EXTRACTED, event.documentId().toString(), event);
    }
}
