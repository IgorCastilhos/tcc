package br.unisinos.cvmsignals.indexing.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChunkingPolicyTest {

    @Test
    void preservaParagrafosQueCabemNaJanela() {
        ChunkingPolicy policy = new ChunkingPolicy(100, 10);

        List<String> chunks = policy.split("Primeiro parágrafo.\n\nSegundo parágrafo.");

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0)).contains("Primeiro parágrafo.").contains("Segundo parágrafo.");
    }

    @Test
    void quebraQuandoJanelaEstoura_comSobreposicaoDoFinalAnterior() {
        ChunkingPolicy policy = new ChunkingPolicy(40, 8);
        String p1 = "A".repeat(30);
        String p2 = "B".repeat(30);

        List<String> chunks = policy.split(p1 + "\n\n" + p2);

        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0)).isEqualTo(p1);
        // O segundo chunk inicia com a cauda do anterior, preservando contexto na fronteira.
        assertThat(chunks.get(1)).startsWith("A".repeat(8)).endsWith(p2);
    }

    @Test
    void paragrafoMaiorQueAJanelaSofreHardSplitComOverlap() {
        ChunkingPolicy policy = new ChunkingPolicy(50, 10);
        String longo = "X".repeat(120);

        List<String> chunks = policy.split(longo);

        assertThat(chunks).allSatisfy(c -> assertThat(c.length()).isLessThanOrEqualTo(50));
        assertThat(String.join("", chunks)).contains("X".repeat(50));
        assertThat(chunks.size()).isGreaterThanOrEqualTo(3);
    }

    @Test
    void ignoraParagrafosVaziosEEspacos() {
        ChunkingPolicy policy = new ChunkingPolicy(100, 0);

        List<String> chunks = policy.split("  \n\n\n\nConteúdo real.\n\n   \n\n");

        assertThat(chunks).containsExactly("Conteúdo real.");
    }

    @Test
    void rejeitaParametrosInvalidos() {
        assertThatThrownBy(() -> new ChunkingPolicy(0, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ChunkingPolicy(100, 100)).isInstanceOf(IllegalArgumentException.class);
    }
}
