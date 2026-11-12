import type { Tone } from '../lib/format';
import { formatConfidence } from '../lib/format';

export function Chip({ tone, children }: { tone: Tone; children: React.ReactNode }) {
  return <span className={`chip chip--${tone}`}>{children}</span>;
}

/**
 * Confiança declarada pelo próprio modelo. Barra + número, porque a barra
 * sozinha não é legível para quem usa leitor de tela.
 */
export function Confidence({ value }: { value: number }) {
  const pct = formatConfidence(value);
  return (
    <span className="confidence" title={`Confiança declarada pelo modelo: ${pct}`}>
      <span className="confidence__track" aria-hidden="true">
        <span className="confidence__fill" style={{ width: `${Math.round(value * 100)}%` }} />
      </span>
      {pct}
    </span>
  );
}
