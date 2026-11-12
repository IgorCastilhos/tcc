package br.unisinos.cvmsignals.signalextraction.infrastructure.messaging;

import br.unisinos.cvmsignals.signalextraction.application.usecase.ExtractSignalsService;
import br.unisinos.cvmsignals.shared.Topics;
import br.unisinos.cvmsignals.shared.events.ChunksEmbedded;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Extração automática ao fim da indexação. Desligável via propriedade
 * (extraction.auto=false) para controlar custo de API durante experimentos.
 */
@Component
public class ChunksEmbeddedListener {

    private static final Logger log = LoggerFactory.getLogger(ChunksEmbeddedListener.class);

    private final ExtractSignalsService extractSignals;
    private final boolean autoExtract;

    public ChunksEmbeddedListener(ExtractSignalsService extractSignals,
                                  @Value("${extraction.auto:false}") boolean autoExtract) {
        this.extractSignals = extractSignals;
        this.autoExtract = autoExtract;
    }

    @KafkaListener(topics = Topics.CHUNKS_EMBEDDED, groupId = "signal-extraction")
    public void onChunksEmbedded(ChunksEmbedded event) {
        if (!autoExtract) {
            log.debug("extraction.auto=false; ignorando {}", event.documentId());
            return;
        }
        int count = extractSignals.extract(event.documentId());
        log.info("Documento {}: {} sinais extraídos", event.documentId(), count);
    }
}
