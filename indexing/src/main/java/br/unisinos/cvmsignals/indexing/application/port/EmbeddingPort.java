package br.unisinos.cvmsignals.indexing.application.port;

import java.util.List;

/** Abstrai o modelo de embedding (BGE-M3 via Ollama, por trás do Spring AI). */
public interface EmbeddingPort {

    List<float[]> embed(List<String> texts);
}
