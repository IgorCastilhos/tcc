package br.unisinos.cvmsignals.retrieval.domain;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReciprocalRankFusionTest {

    private final UUID doc = UUID.randomUUID();

    private ScoredChunk chunk(UUID id, double score) {
        return new ScoredChunk(id, doc, "conteudo", score);
    }

    @Test
    void chunkPresenteNosDoisRankingsSobeParaOTopo() {
        UUID comum = UUID.randomUUID();
        UUID soDenso = UUID.randomUUID();
        UUID soEsparso = UUID.randomUUID();
        ReciprocalRankFusion fusion = new ReciprocalRankFusion(60);

        List<ScoredChunk> fused = fusion.fuse(
                List.of(chunk(soDenso, 0.9), chunk(comum, 0.8)),
                List.of(chunk(soEsparso, 12.0), chunk(comum, 8.0)));

        // score(comum) = 1/62 + 1/62 > 1/61 = score dos exclusivos de cada braço.
        assertThat(fused.get(0).chunkId()).isEqualTo(comum);
        assertThat(fused).hasSize(3);
    }

    @Test
    void scoreFundidoIgnoraEscalasOriginais() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        ReciprocalRankFusion fusion = new ReciprocalRankFusion(60);

        List<ScoredChunk> fused = fusion.fuse(
                List.of(chunk(a, 0.99), chunk(b, 0.98)),
                List.of(chunk(b, 1000.0), chunk(a, 999.0)));

        // Cada um é 1º em um braço e 2º no outro: empate exato em 1/61 + 1/62.
        assertThat(fused.get(0).score()).isEqualTo(fused.get(1).score());
    }

    @Test
    void rankingUnicoPreservaOrdem() {
        UUID primeiro = UUID.randomUUID();
        UUID segundo = UUID.randomUUID();
        ReciprocalRankFusion fusion = new ReciprocalRankFusion(60);

        List<ScoredChunk> fused = fusion.fuse(List.of(chunk(primeiro, 0.9), chunk(segundo, 0.5)));

        assertThat(fused).extracting(ScoredChunk::chunkId).containsExactly(primeiro, segundo);
    }
}
