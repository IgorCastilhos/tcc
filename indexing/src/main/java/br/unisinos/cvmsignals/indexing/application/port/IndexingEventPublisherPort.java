package br.unisinos.cvmsignals.indexing.application.port;

import br.unisinos.cvmsignals.shared.events.ChunksEmbedded;

public interface IndexingEventPublisherPort {

    void publish(ChunksEmbedded event);
}
