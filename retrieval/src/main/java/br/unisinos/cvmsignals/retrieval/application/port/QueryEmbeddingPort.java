package br.unisinos.cvmsignals.retrieval.application.port;

public interface QueryEmbeddingPort {

    float[] embed(String query);
}
