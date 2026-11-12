package br.unisinos.cvmsignals.app;

import br.unisinos.cvmsignals.indexing.domain.Chunk;
import br.unisinos.cvmsignals.indexing.infrastructure.PgVectorChunkRepository;
import br.unisinos.cvmsignals.retrieval.application.RetrievalRequest;
import br.unisinos.cvmsignals.retrieval.application.usecase.HybridRetrievalService;
import br.unisinos.cvmsignals.retrieval.domain.ScoredChunk;
import br.unisinos.cvmsignals.retrieval.infrastructure.PgDenseSearchAdapter;
import br.unisinos.cvmsignals.retrieval.infrastructure.PgSparseSearchAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integração real contra PostgreSQL + pgvector (Testcontainers, sem mocks):
 * valida a migration Flyway, o INSERT com casts ::vector/::jsonb, o índice
 * HNSW (braço denso), o FTS em português (braço esparso), o filtro por
 * documento e a fusão RRF do HybridRetrievalService.
 */
@Testcontainers(disabledWithoutDocker = true)
class PgVectorHybridSearchIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres")).withDatabaseName("cvmsignals").withUsername("cvm").withPassword("cvm");

    static JdbcTemplate jdbc;
    static PgVectorChunkRepository chunkRepository;
    static UUID docDividendos;
    static UUID docOutro;

    @BeforeAll
    static void setUp() {
        Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword()).locations("classpath:db/migration").load().migrate();

        DriverManagerDataSource dataSource = new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        jdbc = new JdbcTemplate(dataSource);
        chunkRepository = new PgVectorChunkRepository(jdbc, new ObjectMapper());

        UUID empresa = UUID.randomUUID();
        jdbc.update("INSERT INTO empresa (id, cnpj, nome) VALUES (?, ?, ?)", empresa, "00000000000191", "Petrobras");
        docDividendos = insertDocumento(empresa, "FATO_RELEVANTE");
        docOutro = insertDocumento(empresa, "ITR");

        chunkRepository.saveAll(List.of(chunk(docDividendos, 0, "O conselho aprovou a distribuição de dividendos extraordinários aos acionistas.", axis(0)), chunk(docOutro, 0, "A produção de petróleo no trimestre permaneceu estável em relação ao anterior.", axis(1))));
    }

    private static UUID insertDocumento(UUID empresa, String tipo) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO documento (id, empresa_id, tipo, data_referencia, url_origem, status)
                VALUES (?, ?, ?, ?, ?, 'PARSED')
                """, id, empresa, tipo, LocalDate.of(2026, 5, 15), "https://cvm.test/" + id);
        return id;
    }

    private static Chunk chunk(UUID documentId, int seq, String content, float[] embedding) {
        return new Chunk(UUID.randomUUID(), documentId, seq, content, embedding, Map.of("cnpj", "00000000000191"));
    }

    /**
     * Vetor unitário no eixo dado: distâncias de cosseno previsíveis para o teste.
     */
    private static float[] axis(int index) {
        float[] vector = new float[1024];
        vector[index] = 1.0f;
        return vector;
    }

    @Test
    void buscaDensaOrdenaPorSimilaridadeDeCosseno() {
        PgDenseSearchAdapter dense = new PgDenseSearchAdapter(jdbc);

        List<ScoredChunk> result = dense.search(RetrievalRequest.of("qualquer", 10), axis(0), 10);

        // Sem asserção sobre o total: a tabela chunk é compartilhada entre os testes.
        assertThat(result).extracting(ScoredChunk::documentId).contains(docDividendos, docOutro);
        assertThat(result.get(0).documentId()).isEqualTo(docDividendos);
        assertThat(result).isSortedAccordingTo(Comparator.comparingDouble(ScoredChunk::score).reversed());
    }

    @Test
    void buscaEsparsaEncontraTermoComStemmingPortugues() {
        PgSparseSearchAdapter sparse = new PgSparseSearchAdapter(jdbc);

        // "acionista" (singular) casa com "acionistas" do documento: ambos reduzem
        // ao radical 'acion'. Note que "dividendo"/"dividendos" NÃO casam — o
        // stemmer português trata '-endo' como gerúndio (ver limitação no README).
        List<ScoredChunk> result = sparse.search(RetrievalRequest.of("acionista", 5), 5);

        assertThat(result).extracting(ScoredChunk::documentId).containsExactly(docDividendos);
    }

    @Test
    void filtroPorDocumentoRestringeOsDoisBracos() {
        PgDenseSearchAdapter dense = new PgDenseSearchAdapter(jdbc);

        List<ScoredChunk> result = dense.search(RetrievalRequest.of("qualquer", 5).forDocument(docOutro), axis(0), 5);

        assertThat(result).extracting(ScoredChunk::documentId).containsExactly(docOutro);
    }

    @Test
    void recuperacaoHibridaFundeBracosEAplicaTopK() {
        HybridRetrievalService retrieval = new HybridRetrievalService(query -> axis(0), new PgDenseSearchAdapter(jdbc), new PgSparseSearchAdapter(jdbc),
                // Reranker de identidade: preserva a ordem da fusão RRF e aplica o corte.
                (query, candidates, topK) -> candidates.stream().sorted(Comparator.comparingDouble(ScoredChunk::score).reversed()).limit(topK).toList(), 60, 20);

        List<ScoredChunk> result = retrieval.retrieve(RetrievalRequest.of("dividendos aos acionistas", 1));

        // O chunk de dividendos lidera os dois braços (denso via eixo 0, esparso via FTS).
        assertThat(result).hasSize(1);
        assertThat(result.get(0).documentId()).isEqualTo(docDividendos);
    }

    @Test
    void reindexacaoSubstituiChunksDoDocumento() {
        UUID temp = insertDocumento(jdbc.queryForObject("SELECT id FROM empresa LIMIT 1", UUID.class), "DFP");
        try {
            chunkRepository.saveAll(List.of(chunk(temp, 0, "versão antiga do texto", axis(2))));

            chunkRepository.deleteByDocumentId(temp);
            chunkRepository.saveAll(List.of(chunk(temp, 0, "versão nova do texto", axis(3))));

            Integer count = jdbc.queryForObject("SELECT count(*) FROM chunk WHERE documento_id = ?", Integer.class, temp);
            assertThat(count).isEqualTo(1);
        } finally {
            jdbc.update("DELETE FROM chunk WHERE documento_id = ?", temp);
            jdbc.update("DELETE FROM documento WHERE id = ?", temp);
        }
    }
}
