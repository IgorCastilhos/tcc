/** Espelha o PanelController.PanelResponse do backend. */

export type SignalLabel = string;

export interface Verdict {
  label: SignalLabel;
  confidence: number;
  justification: string | null;
  evidence: string | null;
}

/** A leitura de um LLM sobre um documento. */
export interface ModelReading {
  model: ModelId;
  sentiment: Verdict | null;
  risk: Verdict | null;
  events: Verdict[];
}

export interface DocumentCard {
  documentId: string;
  type: 'DFP' | 'ITR' | 'FATO_RELEVANTE' | string;
  referenceDate: string;
  /** URL do documento original na CVM — a origem do trecho citado. */
  sourceUrl: string | null;
  /** Protocolo de entrega na CVM, quando o dataset informa. */
  protocol: string | null;
  readings: ModelReading[];
}

export interface PricePoint {
  date: string;
  close: number;
}

export interface Quote {
  ticker: string;
  date: string;
  close: number;
  previousClose: number | null;
  changePct: number | null;
  history: PricePoint[];
}

export interface CompanyCard {
  cnpj: string;
  name: string;
  ticker: string | null;
  /** SEED = semente curada da migration V2; MANUAL = corrigido via API. */
  tickerOrigin: 'SEED' | 'MANUAL' | null;
  quote: Quote | null;
  documents: DocumentCard[];
}

export interface Panel {
  generatedAt: string;
  disclaimer: string;
  statements: string[];
  companies: CompanyCard[];
}

export const MODELS = ['CLAUDE_SONNET', 'GPT_4O', 'GEMMA'] as const;
export type ModelId = (typeof MODELS)[number];

export const MODEL_LABELS: Record<ModelId, string> = {
  CLAUDE_SONNET: 'Claude Sonnet',
  GPT_4O: 'GPT-4o',
  GEMMA: 'Gemma (local)',
};

export const DOCUMENT_LABELS: Record<string, string> = {
  DFP: 'Demonstração Financeira Padronizada',
  ITR: 'Informação Trimestral',
  FATO_RELEVANTE: 'Fato Relevante',
};

export interface Submission {
  participantCode: string;
  week: number;
  answers: Record<number, number>;
  comments: string;
}
