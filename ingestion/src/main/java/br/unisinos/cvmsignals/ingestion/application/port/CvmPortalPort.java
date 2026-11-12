package br.unisinos.cvmsignals.ingestion.application.port;

import br.unisinos.cvmsignals.ingestion.application.DiscoveredDocument;
import br.unisinos.cvmsignals.shared.DocumentType;

import java.util.List;

/** Porta de saída para o Portal de Dados Abertos da CVM. */
public interface CvmPortalPort {

    List<DiscoveredDocument> discover(DocumentType type, int year);
}
