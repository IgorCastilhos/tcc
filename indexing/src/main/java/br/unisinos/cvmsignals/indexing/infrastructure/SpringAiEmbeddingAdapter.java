package br.unisinos.cvmsignals.indexing.infrastructure;

import br.unisinos.cvmsignals.indexing.application.port.EmbeddingPort;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Component;

import java.util.List;

/** Ponte entre a porta de domínio e o EmbeddingModel do Spring AI (Ollama + BGE-M3). */
@Component
public class SpringAiEmbeddingAdapter implements EmbeddingPort {

    private final EmbeddingModel embeddingModel;

    public SpringAiEmbeddingAdapter(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        return embeddingModel.embed(texts);
    }
}
