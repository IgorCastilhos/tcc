package br.unisinos.cvmsignals.ingestion.infrastructure.cvm;

import br.unisinos.cvmsignals.ingestion.application.DiscoveredDocument;
import br.unisinos.cvmsignals.ingestion.application.port.CvmPortalPort;
import br.unisinos.cvmsignals.shared.DocumentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Adaptador do Portal de Dados Abertos da CVM (dados.cvm.gov.br).
 * Fatos Relevantes vêm do dataset IPE; DFP/ITR vêm dos metadados
 * dos datasets periódicos, cujo LINK_DOC aponta para o pacote no ENET.
 * Os CSVs usam separador ';' e charset ISO-8859-1.
 */
@Component
public class CvmOpenDataClient implements CvmPortalPort {

    private static final Logger log = LoggerFactory.getLogger(CvmOpenDataClient.class);
    private static final String FATO_RELEVANTE = "Fato Relevante";

    private final RestClient cvmRestClient;

    public CvmOpenDataClient(@Qualifier("cvmRestClient") RestClient cvmRestClient) {
        this.cvmRestClient = cvmRestClient;
    }

    @Override
    public List<DiscoveredDocument> discover(DocumentType type, int year) {
        return switch (type) {
            case FATO_RELEVANTE -> discoverFromIpe(year);
            case DFP -> discoverPeriodic("/dados/CIA_ABERTA/DOC/DFP/DADOS/dfp_cia_aberta_" + year + ".zip",
                    "dfp_cia_aberta_" + year + ".csv", DocumentType.DFP);
            case ITR -> discoverPeriodic("/dados/CIA_ABERTA/DOC/ITR/DADOS/itr_cia_aberta_" + year + ".zip",
                    "itr_cia_aberta_" + year + ".csv", DocumentType.ITR);
        };
    }

    private List<DiscoveredDocument> discoverFromIpe(int year) {
        List<String[]> rows = fetchCsv("/dados/CIA_ABERTA/DOC/IPE/DADOS/ipe_cia_aberta_" + year + ".zip",
                "ipe_cia_aberta_" + year + ".csv");
        return mapRows(rows, DocumentType.FATO_RELEVANTE, true);
    }

    private List<DiscoveredDocument> discoverPeriodic(String zipPath, String csvName, DocumentType type) {
        return mapRows(fetchCsv(zipPath, csvName), type, false);
    }

    private List<DiscoveredDocument> mapRows(List<String[]> rows, DocumentType type, boolean filterFatoRelevante) {
        if (rows.isEmpty()) {
            return List.of();
        }
        Map<String, Integer> header = indexHeader(rows.get(0));
        int cnpj = firstPresent(header, "CNPJ_Companhia", "CNPJ_CIA");
        int name = firstPresent(header, "Nome_Companhia", "DENOM_CIA");
        int cvmCode = firstPresent(header, "Codigo_CVM", "CD_CVM");
        int refDate = firstPresent(header, "Data_Referencia", "DT_REFER");
        int link = firstPresent(header, "Link_Download", "LINK_DOC");
        Integer category = header.get("Categoria");
        Integer protocol = header.containsKey("Protocolo_Entrega") ? header.get("Protocolo_Entrega") : header.get("ID_DOC");

        List<DiscoveredDocument> result = new ArrayList<>();
        for (int i = 1; i < rows.size(); i++) {
            String[] row = rows.get(i);
            if (row.length <= link || row[link].isBlank()) {
                continue;
            }
            if (filterFatoRelevante && (category == null || !FATO_RELEVANTE.equalsIgnoreCase(row[category].strip()))) {
                continue;
            }
            try {
                result.add(new DiscoveredDocument(
                        row[cnpj].strip(), row[name].strip(), row[cvmCode].strip(), type,
                        LocalDate.parse(row[refDate].strip().substring(0, 10)),
                        row[link].strip(),
                        protocol != null && row.length > protocol ? row[protocol].strip() : null));
            } catch (RuntimeException e) {
                log.warn("Linha {} ignorada no CSV da CVM: {}", i, e.getMessage());
            }
        }
        log.info("CVM: {} documentos {} descobertos", result.size(), type);
        return result;
    }

    private List<String[]> fetchCsv(String zipPath, String csvName) {
        byte[] zip = cvmRestClient.get().uri(zipPath).retrieve().body(byte[].class);
        if (zip == null) {
            throw new IllegalStateException("Resposta vazia da CVM para " + zipPath);
        }
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().equalsIgnoreCase(csvName)) {
                    List<String[]> rows = new ArrayList<>();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(zis, StandardCharsets.ISO_8859_1));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        rows.add(line.split(";", -1));
                    }
                    return rows;
                }
            }
            throw new IllegalStateException("CSV " + csvName + " não encontrado em " + zipPath);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Falha ao ler ZIP da CVM: " + zipPath, e);
        }
    }

    private Map<String, Integer> indexHeader(String[] header) {
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < header.length; i++) {
            index.put(header[i].strip(), i);
        }
        return index;
    }

    private int firstPresent(Map<String, Integer> header, String... candidates) {
        for (String candidate : candidates) {
            Integer idx = header.get(candidate);
            if (idx != null) {
                return idx;
            }
        }
        throw new IllegalStateException("Nenhuma das colunas esperadas presente: " + String.join("/", candidates));
    }
}
