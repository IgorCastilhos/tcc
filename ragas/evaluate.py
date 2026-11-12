"""Avaliação técnica do pipeline com RAGAS (Seção 4.5 da monografia).

Consome o JSONL exportado por GET /api/evaluation/ragas-dataset
(question, answer, contexts, model, signal_type) e calcula faithfulness,
answer_relevancy e context_precision por modelo. context_recall exige
ground_truth — inclua o campo no JSONL (anotação manual) para habilitá-la.

Uso:
    python evaluate.py dataset.jsonl saida_prefix
Requer OPENAI_API_KEY (LLM juiz do RAGAS).
"""
import json
import sys

import pandas as pd
from datasets import Dataset
from ragas import evaluate
from ragas.metrics import (
    answer_relevancy,
    context_precision,
    context_recall,
    faithfulness,
)


def load_rows(path: str) -> list[dict]:
    rows = []
    with open(path, encoding="utf-8") as handle:
        for line in handle:
            line = line.strip()
            if line:
                rows.append(json.loads(line))
    if not rows:
        raise SystemExit(f"Dataset vazio: {path}")
    return rows


def select_metrics(rows: list[dict]) -> list:
    metrics = [faithfulness, answer_relevancy, context_precision]
    if all("ground_truth" in row and row["ground_truth"] for row in rows):
        metrics.append(context_recall)
    else:
        print("Aviso: sem ground_truth em todas as linhas; context_recall omitida.")
    return metrics


def main(dataset_path: str, output_prefix: str) -> None:
    rows = load_rows(dataset_path)
    dataset = Dataset.from_list(rows)
    result = evaluate(dataset, metrics=select_metrics(rows))

    frame: pd.DataFrame = result.to_pandas()
    if "model" in frame.columns:
        by_model = frame.groupby("model").mean(numeric_only=True)
        print("\n=== Médias por modelo ===")
        print(by_model.round(4).to_string())
        by_model.to_csv(f"{output_prefix}_by_model.csv")

    frame.to_csv(f"{output_prefix}_detailed.csv", index=False)
    with open(f"{output_prefix}_summary.json", "w", encoding="utf-8") as handle:
        json.dump({k: float(v) for k, v in result.items()}, handle, ensure_ascii=False, indent=2)
    print(f"\nRelatórios gravados em {output_prefix}_*.csv/json")


if __name__ == "__main__":
    if len(sys.argv) != 3:
        raise SystemExit(__doc__)
    main(sys.argv[1], sys.argv[2])
