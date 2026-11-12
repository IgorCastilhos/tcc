import type { Panel, Submission } from '../types';

/**
 * A interface roda em dois modos:
 *
 *  - ao vivo, contra a aplicação Spring (VITE_API_BASE vazio usa a mesma
 *    origem, que é o caso quando o bundle é servido de static/);
 *  - congelada, a partir de um snapshot JSON publicado junto ao build —
 *    é assim que os participantes acessam sem expor o Postgres nem as
 *    chaves de API.
 *
 * VITE_SNAPSHOT=1 força o modo congelado.
 */
const API_BASE = (import.meta.env.VITE_API_BASE ?? '').replace(/\/$/, '');
const SNAPSHOT_ONLY = import.meta.env.VITE_SNAPSHOT === '1';
const SNAPSHOT_URL = `${import.meta.env.BASE_URL}data/panel.json`;

export const isSnapshotMode = SNAPSHOT_ONLY;

/** De onde os dados vieram — a interface avisa quando não são os ao vivo. */
export type PanelSource = 'live' | 'snapshot' | 'snapshot-fallback';

export interface LoadedPanel {
  panel: Panel;
  source: PanelSource;
}

export async function loadPanel(): Promise<LoadedPanel> {
  if (SNAPSHOT_ONLY) {
    return { panel: await fetchJson<Panel>(SNAPSHOT_URL), source: 'snapshot' };
  }
  try {
    return { panel: await fetchJson<Panel>(`${API_BASE}/api/panel`), source: 'live' };
  } catch (liveError) {
    // Sem backend alcançável, tenta o snapshot antes de desistir: é o que
    // permite abrir o build estático em qualquer lugar. O resultado vem
    // marcado como fallback porque pode estar desatualizado, e exibir dado
    // velho sem aviso num artefato de pesquisa seria enganoso.
    try {
      return { panel: await fetchJson<Panel>(SNAPSHOT_URL), source: 'snapshot-fallback' };
    } catch {
      throw liveError;
    }
  }
}

export type SubmitResult =
  | { kind: 'sent' }
  | { kind: 'offline'; payload: Submission };

/**
 * Envia as respostas do Quadro 4 para {@code POST /api/evaluation}.
 *
 * <p>A interface não usa mais este caminho: o questionário passou a ser o
 * formulário do Google (ver GoogleForm.tsx). A função segue aqui porque o
 * endpoint continua existindo no backend e porque o questionário embutido
 * original está preservado em `web/legacy/Questionnaire.tsx` — quem quiser
 * voltar a coletar dentro da aplicação tem os dois lados prontos.
 */
export async function submitEvaluation(submission: Submission): Promise<SubmitResult> {
  if (SNAPSHOT_ONLY) {
    return { kind: 'offline', payload: submission };
  }
  const response = await fetch(`${API_BASE}/api/evaluation`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(submission),
  }).catch(() => null);

  if (!response || !response.ok) {
    return { kind: 'offline', payload: submission };
  }
  return { kind: 'sent' };
}

async function fetchJson<T>(url: string): Promise<T> {
  const response = await fetch(url, { headers: { Accept: 'application/json' } });
  if (!response.ok) {
    throw new Error(`${response.status} ao carregar ${url}`);
  }
  return (await response.json()) as T;
}
