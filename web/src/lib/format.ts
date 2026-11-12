import { DOCUMENT_LABELS } from '../types';

/** Tom visual de cada rótulo. O texto sempre acompanha a cor — nunca só cor. */
export type Tone = 'positivo' | 'neutro' | 'negativo' | 'info';

const SENTIMENT_TONE: Record<string, Tone> = {
  POSITIVO: 'positivo',
  NEUTRO: 'neutro',
  NEGATIVO: 'negativo',
};

/** Risco baixo é o cenário confortável, por isso divide o tom com sentimento positivo. */
const RISK_TONE: Record<string, Tone> = {
  BAIXO: 'positivo',
  MEDIO: 'neutro',
  ALTO: 'negativo',
};

export function sentimentTone(label: string | undefined): Tone {
  return (label && SENTIMENT_TONE[label]) || 'info';
}

export function riskTone(label: string | undefined): Tone {
  return (label && RISK_TONE[label]) || 'info';
}

const EVENT_LABELS: Record<string, string> = {
  AQUISICAO: 'Aquisição',
  FUSAO: 'Fusão',
  VENDA_ATIVO: 'Venda de ativo',
  EMISSAO: 'Emissão',
  DIVIDENDOS: 'Dividendos',
  RECOMPRA: 'Recompra',
  MUDANCA_CONTROLE: 'Mudança de controle',
  MUDANCA_ADMINISTRACAO: 'Mudança na administração',
  JUDICIAL_REGULATORIO: 'Judicial / regulatório',
  OUTRO: 'Outro',
};

export function eventLabel(code: string): string {
  return EVENT_LABELS[code] ?? humanize(code);
}

export function documentLabel(type: string): string {
  return DOCUMENT_LABELS[type] ?? humanize(type);
}

function humanize(code: string): string {
  const lower = code.replace(/_/g, ' ').toLowerCase();
  return lower.charAt(0).toUpperCase() + lower.slice(1);
}

export function formatCnpj(cnpj: string): string {
  if (cnpj?.length !== 14) return cnpj ?? '';
  return `${cnpj.slice(0, 2)}.${cnpj.slice(2, 5)}.${cnpj.slice(5, 8)}/${cnpj.slice(8, 12)}-${cnpj.slice(12)}`;
}

const BRL = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });

export function formatMoney(value: number): string {
  return BRL.format(value);
}

export function formatPercent(value: number): string {
  return `${value > 0 ? '+' : ''}${value.toLocaleString('pt-BR', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })}%`;
}

export function formatDate(iso: string): string {
  const [year, month, day] = iso.split('-');
  return day && month && year ? `${day}/${month}/${year}` : iso;
}

export function formatDateTime(iso: string): string {
  const date = new Date(iso);
  return Number.isNaN(date.getTime())
    ? iso
    : date.toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' });
}

export function formatConfidence(confidence: number): string {
  return `${Math.round(confidence * 100)}%`;
}

/**
 * Devolve a URL apenas se ela for segura para virar um `href`.
 *
 * A origem do documento vem do dataset da CVM, ou seja, de fora. O React
 * escapa texto, mas **não** bloqueia `href="javascript:..."` — uma URL
 * adulterada viraria execução de script no clique do participante. Aqui só
 * passam http e https.
 */
export function safeExternalUrl(url: string | null | undefined): string | null {
  if (!url) return null;
  try {
    const parsed = new URL(url, window.location.origin);
    return parsed.protocol === 'https:' || parsed.protocol === 'http:' ? parsed.href : null;
  } catch {
    return null;
  }
}
