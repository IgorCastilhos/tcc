package br.unisinos.cvmsignals.ingestion.infrastructure.messaging;

import br.unisinos.cvmsignals.ingestion.application.usecase.ParseDocumentService;
import br.unisinos.cvmsignals.shared.Topics;
import br.unisinos.cvmsignals.shared.events.DocumentDiscovered;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Consome cvm.docs.discovered e dispara download + parsing de forma assíncrona. */
@Component
public class DocumentDiscoveredListener {

    private static final Logger log = LoggerFactory.getLogger(DocumentDiscoveredListener.class);

    private final ParseDocumentService parseDocument;

    public DocumentDiscoveredListener(ParseDocumentService parseDocument) {
        this.parseDocument = parseDocument;
    }

    @KafkaListener(topics = Topics.DOCS_DISCOVERED, groupId = "ingestion-parser")
    public void onDiscovered(DocumentDiscovered event) {
        log.info("Parsing documento {} ({}) de {}", event.documentId(), event.type(), event.companyName());
        parseDocument.parse(event.documentId(), event.cnpj());
    }
}
