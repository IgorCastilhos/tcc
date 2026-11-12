package br.unisinos.cvmsignals.signalextraction.application.port;

import br.unisinos.cvmsignals.shared.events.SignalsExtracted;

public interface SignalEventPublisherPort {

    void publish(SignalsExtracted event);
}
