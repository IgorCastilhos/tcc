package br.unisinos.cvmsignals.evaluation.application.port;

import java.util.List;

/**
 * Correção manual do mapa CNPJ → código B3. A semente da migration V2 é
 * curada e pode conter erro; esta porta permite ajustar sem editar SQL.
 */
public interface TickerMappingPort {

    /** Grava o ticker com origem MANUAL e propaga para a empresa. */
    void assign(String cnpj, String ticker, String referenceName);

    /** O catálogo de companhias da B3 conhecidas, para orientar a ingestão. */
    List<TickerEntry> list();

    record TickerEntry(String cnpj, String ticker, String referenceName, String origin,
                       boolean ingested) {
    }
}
