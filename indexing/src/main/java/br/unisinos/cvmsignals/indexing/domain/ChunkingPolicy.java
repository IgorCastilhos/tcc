package br.unisinos.cvmsignals.indexing.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Segmentação orientada a parágrafos com janela máxima e sobreposição.
 * Parágrafos são a unidade semântica natural de DFPs e Fatos Relevantes;
 * a sobreposição preserva contexto nas fronteiras, e o limite de tamanho
 * respeita a janela de 8.192 tokens do BGE-M3 com folga.
 */
public final class ChunkingPolicy {

    private final int maxChars;
    private final int overlapChars;

    public ChunkingPolicy(int maxChars, int overlapChars) {
        if (maxChars <= 0 || overlapChars < 0 || overlapChars >= maxChars) {
            throw new IllegalArgumentException("Parâmetros de chunking inválidos");
        }
        this.maxChars = maxChars;
        this.overlapChars = overlapChars;
    }

    public List<String> split(String text) {
        List<String> paragraphs = splitParagraphs(text);
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String paragraph : paragraphs) {
            if (paragraph.length() > maxChars) {
                flush(chunks, current);
                hardSplit(paragraph, chunks);
                continue;
            }
            if (current.length() + paragraph.length() + 1 > maxChars) {
                flush(chunks, current);
                current.append(tailOverlap(chunks));
            }
            if (!current.isEmpty()) {
                current.append('\n');
            }
            current.append(paragraph);
        }
        flush(chunks, current);
        return chunks;
    }

    private List<String> splitParagraphs(String text) {
        List<String> paragraphs = new ArrayList<>();
        for (String candidate : text.split("\\n{2,}")) {
            String trimmed = candidate.strip();
            if (!trimmed.isEmpty()) {
                paragraphs.add(trimmed);
            }
        }
        return paragraphs;
    }

    private void hardSplit(String paragraph, List<String> chunks) {
        int start = 0;
        while (start < paragraph.length()) {
            int end = Math.min(start + maxChars, paragraph.length());
            chunks.add(paragraph.substring(start, end));
            if (end == paragraph.length()) {
                break;
            }
            start = end - overlapChars;
        }
    }

    private String tailOverlap(List<String> chunks) {
        if (overlapChars == 0 || chunks.isEmpty()) {
            return "";
        }
        String last = chunks.get(chunks.size() - 1);
        return last.substring(Math.max(0, last.length() - overlapChars));
    }

    private void flush(List<String> chunks, StringBuilder current) {
        if (!current.isEmpty()) {
            chunks.add(current.toString().strip());
            current.setLength(0);
        }
    }
}
