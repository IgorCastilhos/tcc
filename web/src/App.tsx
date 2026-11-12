import { useCallback, useEffect, useMemo, useState } from 'react';
import type { Panel } from './types';
import type { PanelSource } from './lib/api';
import { loadPanel } from './lib/api';
import { formatDateTime } from './lib/format';
import { CompanyCardView } from './components/CompanyCard';
import { GoogleForm } from './components/GoogleForm';
import { Tour } from './components/Tour';
import { TOUR_STEPS, hasSeenTour, markTourSeen } from './lib/tour';

type Tab = 'empresas' | 'questionario';

type Load =
  | { kind: 'loading' }
  | { kind: 'ready'; panel: Panel; source: PanelSource }
  | { kind: 'error'; message: string };

export function App() {
  const [state, setState] = useState<Load>({ kind: 'loading' });
  const [tab, setTab] = useState<Tab>('empresas');
  const [query, setQuery] = useState('');
  const [tourOpen, setTourOpen] = useState(false);
  const [tourExpandsCard, setTourExpandsCard] = useState(false);

  useEffect(() => {
    let active = true;
    loadPanel()
      .then(({ panel, source }) => active && setState({ kind: 'ready', panel, source }))
      .catch((error: Error) => active && setState({ kind: 'error', message: error.message }));
    return () => {
      active = false;
    };
  }, []);

  // Só depois dos dados: os passos apontam para cards que precisam existir.
  useEffect(() => {
    if (state.kind === 'ready' && !hasSeenTour()) {
      setTourOpen(true);
    }
  }, [state.kind]);

  const closeTour = useCallback(() => {
    markTourSeen();
    setTourOpen(false);
    setTourExpandsCard(false);
  }, []);

  const openTour = useCallback(() => {
    setTab('empresas');
    setQuery('');
    setTourOpen(true);
  }, []);

  const companies = state.kind === 'ready' ? state.panel.companies : [];

  const filtered = useMemo(() => {
    const term = query.trim().toLowerCase();
    if (!term) return companies;
    return companies.filter(
      (company) =>
        company.name.toLowerCase().includes(term) ||
        (company.ticker ?? '').toLowerCase().includes(term) ||
        company.cnpj.includes(term.replace(/\D/g, '')),
    );
  }, [companies, query]);

  const tourSteps = useMemo(
    () => TOUR_STEPS.filter((s) => !s.requiresCompany || companies.length > 0),
    [companies.length],
  );

  return (
    <>
      <header className="topbar">
        <div className="topbar__inner">
          <div className="brand">
            <span className="brand__name">Pipeline de Extração de Sinais Financeiros</span>
            <span className="brand__sub">TCC de Igor Paprocki de Castilhos</span>
          </div>
          <div className="tabs" role="tablist">
            <button
              role="tab"
              className="tab"
              aria-selected={tab === 'empresas'}
              onClick={() => setTab('empresas')}
            >
              Empresas
            </button>
            <button
              role="tab"
              className="tab"
              data-tour="aba-questionario"
              aria-selected={tab === 'questionario'}
              onClick={() => setTab('questionario')}
            >
              Questionário
            </button>
          </div>
          <button
            type="button"
            className="help"
            onClick={openTour}
            title="Rever a apresentação"
            aria-label="Rever a apresentação da interface"
          >
            <svg width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden="true">
              <circle cx="8" cy="8" r="6.6" stroke="currentColor" strokeWidth="1.5" />
              <path
                d="M6.2 6.1a1.85 1.85 0 1 1 2.3 1.79c-.35.1-.5.33-.5.68v.36"
                stroke="currentColor"
                strokeWidth="1.5"
                strokeLinecap="round"
              />
              <circle cx="8" cy="11.4" r="0.85" fill="currentColor" />
            </svg>
            Como usar
          </button>
        </div>
      </header>

      <main className="shell">
        {state.kind === 'ready' && (
          <p className="disclaimer" data-tour="aviso">
            <span aria-hidden="true">⚠️</span>
            <span>
              <strong>Não é recomendação de investimento.</strong> {state.panel.disclaimer}
            </span>
          </p>
        )}

        {state.kind === 'ready' && state.source === 'snapshot-fallback' && (
          <p className="disclaimer" role="status">
            <span aria-hidden="true">📴</span>
            <span>
              <strong>Dados possivelmente desatualizados.</strong> A aplicação não respondeu, então
              esta página está lendo o último snapshot salvo, gerado em{' '}
              {formatDateTime(state.panel.generatedAt)}.
            </span>
          </p>
        )}

        {tab === 'empresas' ? (
          <>
            <div className="toolbar">
              <label className="search" data-tour="busca">
                <svg width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden="true">
                  <circle cx="7" cy="7" r="4.5" stroke="currentColor" strokeWidth="1.6" />
                  <path d="M10.5 10.5L14 14" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
                </svg>
                <input
                  type="search"
                  placeholder="Buscar por empresa, código B3 ou CNPJ"
                  value={query}
                  onChange={(e) => setQuery(e.target.value)}
                  aria-label="Buscar empresa"
                />
              </label>
              {state.kind === 'ready' && (
                <span className="count">
                  {filtered.length} de {companies.length}{' '}
                  {companies.length === 1 ? 'empresa' : 'empresas'}
                </span>
              )}
            </div>

            {state.kind === 'loading' && (
              <div aria-live="polite" aria-busy="true">
                <span className="sr-only">Carregando sinais…</span>
                <div className="skeleton" />
                <div className="skeleton" />
                <div className="skeleton" />
              </div>
            )}

            {state.kind === 'error' && (
              <div className="state">
                <p className="state__title">Não foi possível carregar os sinais</p>
                <p>{state.message}</p>
                <p>
                  Se você está rodando localmente, confirme que a aplicação está no ar em{' '}
                  <code>localhost:8080</code>.
                </p>
              </div>
            )}

            {state.kind === 'ready' && filtered.length === 0 && (
              <div className="state">
                <p className="state__title">
                  {companies.length === 0 ? 'Nenhum sinal extraído ainda' : 'Nenhuma empresa encontrada'}
                </p>
                <p>
                  {companies.length === 0
                    ? 'Rode a ingestão e a extração no pipeline para popular o painel.'
                    : 'Tente outro termo de busca.'}
                </p>
              </div>
            )}

            {state.kind === 'ready' && filtered.length > 0 && (
              <div className="cards">
                {filtered.map((company, i) => (
                  <CompanyCardView
                    key={company.cnpj}
                    company={company}
                    tourAnchor={i === 0}
                    forceOpen={i === 0 && tourExpandsCard}
                  />
                ))}
              </div>
            )}
          </>
        ) : (
          <GoogleForm />
        )}

        {tourOpen && state.kind === 'ready' && (
          <Tour steps={tourSteps} onFinish={closeTour} onExpandFirstCard={setTourExpandsCard} />
        )}

        <footer className="footer">
          <p>
            Artefato do TCC — Unisinos, 2026. Sinais extraídos de documentos públicos da CVM
            (DFP, ITR e Fatos Relevantes) por Claude Sonnet, GPT-4o e Gemma sob o mesmo contexto
            e os mesmos prompts.
          </p>
          {state.kind === 'ready' && <p>Dados gerados em {formatDateTime(state.panel.generatedAt)}.</p>}
        </footer>
      </main>
    </>
  );
}
