package br.unisinos.cvmsignals.retrieval.infrastructure;

import br.unisinos.cvmsignals.retrieval.application.port.QueryEmbeddingPort;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Component;

@Component
public class SpringAiQueryEmbeddingAdapter implements QueryEmbeddingPort {

    private final EmbeddingModel embeddingModel;

    public SpringAiQueryEmbeddingAdapter(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    @Override
    public float[] embed(String query) {
        return embeddingModel.embed(query);
    }
}
