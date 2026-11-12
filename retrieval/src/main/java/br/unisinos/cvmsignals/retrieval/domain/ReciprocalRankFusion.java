package br.unisinos.cvmsignals.retrieval.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Fusão RRF: score(c) = Σ 1/(k + rank_i(c)). Combina os rankings denso e
 * esparso sem exigir calibração entre escalas de similaridade distintas —
 * exatamente o problema de fundir cosseno (pgvector) com ts_rank (FTS).
 */
public final class ReciprocalRankFusion {

    private final int k;

    public ReciprocalRankFusion(int k) {
        if (k <= 0) {
            throw new IllegalArgumentException("k deve ser positivo");
        }
        this.k = k;
    }

    @SafeVarargs
    public final List<ScoredChunk> fuse(List<ScoredChunk>... rankings) {
        Map<UUID, ScoredChunk> byId = new LinkedHashMap<>();
        Map<UUID, Double> fusedScores = new LinkedHashMap<>();

        for (List<ScoredChunk> ranking : rankings) {
            for (int rank = 0; rank < ranking.size(); rank++) {
                ScoredChunk chunk = ranking.get(rank);
                byId.putIfAbsent(chunk.chunkId(), chunk);
                fusedScores.merge(chunk.chunkId(), 1.0 / (k + rank + 1), Double::sum);
            }
        }

        List<ScoredChunk> fused = new ArrayList<>(byId.size());
        for (Map.Entry<UUID, Double> entry : fusedScores.entrySet()) {
            fused.add(byId.get(entry.getKey()).withScore(entry.getValue()));
        }
        fused.sort(Comparator.comparingDouble(ScoredChunk::score).reversed());
        return fused;
    }
}
