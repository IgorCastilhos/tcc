package br.unisinos.cvmsignals.indexing.infrastructure;

import br.unisinos.cvmsignals.indexing.application.usecase.IndexDocumentService;
import br.unisinos.cvmsignals.shared.Topics;
import br.unisinos.cvmsignals.shared.events.DocumentParsed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class DocumentParsedListener {

    private static final Logger log = LoggerFactory.getLogger(DocumentParsedListener.class);

    private final IndexDocumentService indexDocument;

    public DocumentParsedListener(IndexDocumentService indexDocument) {
        this.indexDocument = indexDocument;
    }

    @KafkaListener(topics = Topics.DOCS_PARSED, groupId = "indexing")
    public void onParsed(DocumentParsed event) {
        int count = indexDocument.index(event.documentId());
        log.info("Documento {} indexado em {} chunks", event.documentId(), count);
    }
}
