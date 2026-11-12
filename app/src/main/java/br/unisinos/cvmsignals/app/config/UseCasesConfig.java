package br.unisinos.cvmsignals.app.config;

import br.unisinos.cvmsignals.evaluation.application.port.EvaluationRepositoryPort;
import br.unisinos.cvmsignals.evaluation.application.usecase.EvaluationReportService;
import br.unisinos.cvmsignals.evaluation.application.usecase.SubmitEvaluationService;
import br.unisinos.cvmsignals.indexing.application.port.ChunkRepositoryPort;
import br.unisinos.cvmsignals.indexing.application.port.EmbeddingPort;
import br.unisinos.cvmsignals.indexing.application.port.IndexingEventPublisherPort;
import br.unisinos.cvmsignals.indexing.application.port.ParsedDocumentPort;
import br.unisinos.cvmsignals.indexing.application.usecase.IndexDocumentService;
import br.unisinos.cvmsignals.indexing.domain.ChunkingPolicy;
import br.unisinos.cvmsignals.ingestion.application.port.CompanyRepositoryPort;
import br.unisinos.cvmsignals.ingestion.application.port.CvmPortalPort;
import br.unisinos.cvmsignals.ingestion.application.port.DocumentDownloadPort;
import br.unisinos.cvmsignals.ingestion.application.port.DocumentParserPort;
import br.unisinos.cvmsignals.ingestion.application.port.DocumentRepositoryPort;
import br.unisinos.cvmsignals.ingestion.application.port.IngestionEventPublisherPort;
import br.unisinos.cvmsignals.ingestion.application.usecase.DiscoverDocumentsService;
import br.unisinos.cvmsignals.ingestion.application.usecase.ParseDocumentService;
import br.unisinos.cvmsignals.ingestion.application.usecase.RetryFailedDocumentsService;
import br.unisinos.cvmsignals.marketdata.application.port.PriceRepositoryPort;
import br.unisinos.cvmsignals.marketdata.application.port.QuoteProviderPort;
import br.unisinos.cvmsignals.marketdata.application.usecase.SyncPricesService;
import br.unisinos.cvmsignals.retrieval.application.port.DenseSearchPort;
import br.unisinos.cvmsignals.retrieval.application.port.QueryEmbeddingPort;
import br.unisinos.cvmsignals.retrieval.application.port.RerankPort;
import br.unisinos.cvmsignals.retrieval.application.port.SparseSearchPort;
import br.unisinos.cvmsignals.retrieval.application.usecase.HybridRetrievalService;
import br.unisinos.cvmsignals.signalextraction.application.port.DocumentContextPort;
import br.unisinos.cvmsignals.signalextraction.application.port.LlmExtractionPort;
import br.unisinos.cvmsignals.signalextraction.application.port.SignalEventPublisherPort;
import br.unisinos.cvmsignals.signalextraction.application.port.SignalRepositoryPort;
import br.unisinos.cvmsignals.signalextraction.application.usecase.ExtractSignalsService;
import br.unisinos.cvmsignals.signalextraction.domain.LlmProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

/**
 * Composition root: os casos de uso são POJOs puros (sem anotações Spring),
 * então o wiring acontece aqui — a camada de aplicação permanece livre de
 * framework, verificável pelo ArchitectureTest.
 */
@Configuration
public class UseCasesConfig {

    @Bean
    public DiscoverDocumentsService discoverDocumentsService(CvmPortalPort cvmPortal,
                                                             CompanyRepositoryPort companies,
                                                             DocumentRepositoryPort documents,
                                                             IngestionEventPublisherPort events) {
        return new DiscoverDocumentsService(cvmPortal, companies, documents, events);
    }

    @Bean
    public ParseDocumentService parseDocumentService(DocumentRepositoryPort documents,
                                                     DocumentDownloadPort downloader,
                                                     DocumentParserPort parser,
                                                     IngestionEventPublisherPort events) {
        return new ParseDocumentService(documents, downloader, parser, events);
    }

    @Bean
    public RetryFailedDocumentsService retryFailedDocumentsService(DocumentRepositoryPort documents,
                                                                   ParseDocumentService parseDocument,
                                                                   @Value("${cvm.download.retry-pause-ms:900}") long pauseMillis) {
        return new RetryFailedDocumentsService(documents, parseDocument, pauseMillis);
    }

    @Bean
    public ChunkingPolicy chunkingPolicy(@Value("${indexing.chunk.max-chars:1800}") int maxChars,
                                         @Value("${indexing.chunk.overlap-chars:200}") int overlapChars) {
        return new ChunkingPolicy(maxChars, overlapChars);
    }

    @Bean
    public IndexDocumentService indexDocumentService(ParsedDocumentPort parsedDocuments,
                                                     ChunkRepositoryPort chunks,
                                                     EmbeddingPort embeddings,
                                                     IndexingEventPublisherPort events,
                                                     ChunkingPolicy chunkingPolicy) {
        return new IndexDocumentService(parsedDocuments, chunks, embeddings, events, chunkingPolicy);
    }

    @Bean
    public HybridRetrievalService hybridRetrievalService(QueryEmbeddingPort queryEmbedding,
                                                         DenseSearchPort denseSearch,
                                                         SparseSearchPort sparseSearch,
                                                         RerankPort reranker,
                                                         @Value("${retrieval.rrf-k:60}") int rrfK,
                                                         @Value("${retrieval.candidates-per-arm:20}") int candidatesPerArm) {
        return new HybridRetrievalService(queryEmbedding, denseSearch, sparseSearch, reranker, rrfK, candidatesPerArm);
    }

    @Bean
    public ExtractSignalsService extractSignalsService(DocumentContextPort documentContext,
                                                       LlmExtractionPort llm,
                                                       SignalRepositoryPort signals,
                                                       SignalEventPublisherPort events,
                                                       @Value("${extraction.providers:CLAUDE_SONNET,GPT_4O,GEMMA}") String providers,
                                                       @Value("${extraction.context-top-k:5}") int contextTopK) {
        List<LlmProvider> enabled = Arrays.stream(providers.split(","))
                .map(String::strip)
                .map(LlmProvider::valueOf)
                .toList();
        return new ExtractSignalsService(documentContext, llm, signals, events, enabled, contextTopK);
    }

    @Bean
    public SyncPricesService syncPricesService(QuoteProviderPort quotes, PriceRepositoryPort prices) {
        return new SyncPricesService(quotes, prices);
    }

    @Bean
    public SubmitEvaluationService submitEvaluationService(EvaluationRepositoryPort evaluations) {
        return new SubmitEvaluationService(evaluations);
    }

    @Bean
    public EvaluationReportService evaluationReportService(EvaluationRepositoryPort evaluations) {
        return new EvaluationReportService(evaluations);
    }
}
