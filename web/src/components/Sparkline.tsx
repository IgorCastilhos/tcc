import type { PricePoint } from '../types';

interface Props {
  points: PricePoint[];
  width?: number;
  height?: number;
}

/**
 * Miniatura da série de fechamentos. É decorativa (aria-hidden): o valor e a
 * variação já aparecem em texto ao lado, então o leitor de tela não perde nada.
 */
export function Sparkline({ points, width = 84, height = 30 }: Props) {
  if (points.length < 2) return null;

  const closes = points.map((p) => p.close);
  const min = Math.min(...closes);
  const max = Math.max(...closes);
  const span = max - min || 1;
  const step = width / (points.length - 1);

  const path = closes
    .map((close, i) => {
      const x = i * step;
      const y = height - ((close - min) / span) * (height - 4) - 2;
      return `${i === 0 ? 'M' : 'L'}${x.toFixed(1)},${y.toFixed(1)}`;
    })
    .join(' ');

  const rising = closes[closes.length - 1] >= closes[0];

  return (
    <svg
      className="spark"
      width={width}
      height={height}
      viewBox={`0 0 ${width} ${height}`}
      aria-hidden="true"
      focusable="false"
    >
      <path
        d={path}
        fill="none"
        stroke={rising ? 'var(--positivo)' : 'var(--negativo)'}
        strokeWidth="1.75"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}
