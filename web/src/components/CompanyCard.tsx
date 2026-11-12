import { useState } from 'react';
import type { CompanyCard as Company, DocumentCard, ModelReading } from '../types';
import { MODELS, MODEL_LABELS } from '../types';
import {
  documentLabel,
  formatCnpj,
  formatDate,
  formatMoney,
  formatPercent,
  riskTone,
  sentimentTone,
} from '../lib/format';
import { ModelReadingCard } from './ModelReading';
import { Sparkline } from './Sparkline';

/** Colunas sempre na mesma ordem, mesmo que um braço não tenha respondido. */
function orderedReadings(readings: ModelReading[]): ModelReading[] {
  return MODELS.map(
    (model) =>
      readings.find((r) => r.model === model) ?? {
        model,
        sentiment: null,
        risk: null,
        events: [],
      },
  );
}

function QuoteBlock({ company }: { company: Company }) {
  const { quote } = company;
  if (!quote) {
    return (
      <div className="quote quote--empty">
        {company.ticker
          ? 'Cotação não disponível'
          : 'Sem código B3 associado a este CNPJ'}
      </div>
    );
  }

  const change = quote.changePct;
  const direction = change == null || change === 0 ? 'flat' : change > 0 ? 'up' : 'down';
  const arrow = direction === 'up' ? '▲' : direction === 'down' ? '▼' : '–';

  return (
    <div className="quote">
      <div className="quote__value">{formatMoney(quote.close)}</div>
      <div className={`quote__change quote__change--${direction}`}>
        {change == null ? 'sem variação apurada' : `${arrow} ${formatPercent(change)}`}
      </div>
      <div className="quote__date">fech. {formatDate(quote.date)}</div>
    </div>
  );
}

/** Resumo dos três modelos com o card fechado: dois pontos por modelo. */
function Consensus({ readings }: { readings: ModelReading[] }) {
  return (
    <div className="consensus">
      {orderedReadings(readings).map((reading) => {
        const sentiment = reading.sentiment?.label;
        const risk = reading.risk?.label;
        const description = `${MODEL_LABELS[reading.model]}: sentimento ${
          sentiment ?? 'não informado'
        }, risco ${risk ?? 'não informado'}`;
        return (
          <div className="consensus__cell" key={reading.model} title={description}>
            <span className="consensus__model">
              {MODEL_LABELS[reading.model].split(' ')[0]}
            </span>
            <span className="consensus__dots">
              <span className={`dot dot--${sentimentTone(sentiment)}`} />
              <span className={`dot dot--${riskTone(risk)}`} />
            </span>
            <span className="sr-only">{description}</span>
          </div>
        );
      })}
    </div>
  );
}

function DocumentBlock({ document, tourAnchor = false }: { document: DocumentCard; tourAnchor?: boolean }) {
  return (
    <section className="doc">
      <div className="doc__head">
        <span className="doc__type">{documentLabel(document.type)}</span>
        <span className="doc__date">referência {formatDate(document.referenceDate)}</span>
        <span className="pill">3 modelos, mesmo contexto</span>
      </div>
      <div className="readings" data-tour={tourAnchor ? 'leituras' : undefined}>
        {orderedReadings(document.readings).map((reading, i) => (
          <ModelReadingCard
            key={reading.model}
            reading={reading}
            document={document}
            tourAnchor={tourAnchor && i === 0}
          />
        ))}
      </div>
    </section>
  );
}

interface CardProps {
  company: Company;
  /** O tour abre o primeiro card para poder destacar o comparativo. */
  forceOpen?: boolean;
  /** Só o primeiro card recebe os âncoras do tour, para não duplicar seletores. */
  tourAnchor?: boolean;
}

export function CompanyCardView({ company, forceOpen = false, tourAnchor = false }: CardProps) {
  const [open, setOpen] = useState(false);
  const expanded = open || forceOpen;
  const latest = company.documents[0];
  const documentCount = company.documents.length;
  const panelId = `empresa-${company.cnpj}`;

  return (
    <div className="card">
      <button
        type="button"
        className="card__head"
        aria-expanded={expanded}
        aria-controls={panelId}
        onClick={() => setOpen((v) => !v)}
      >
        <div className="identity" data-tour={tourAnchor ? 'identidade' : undefined}>
          <div className="identity__top">
            {company.ticker ? (
              <span className="ticker">{company.ticker}</span>
            ) : (
              <span className="ticker ticker--none">sem ticker</span>
            )}
            <span className="identity__name">{company.name}</span>
          </div>
          <div className="identity__meta">
            <span>CNPJ {formatCnpj(company.cnpj)}</span>
            <span>
              {documentCount} {documentCount === 1 ? 'documento' : 'documentos'}
            </span>
            {latest && <span>{documentLabel(latest.type)} de {formatDate(latest.referenceDate)}</span>}
          </div>
        </div>

        <span data-tour={tourAnchor ? 'cotacao' : undefined} className="quote-anchor">
          {company.quote && <Sparkline points={company.quote.history} />}
          <QuoteBlock company={company} />
        </span>
        {latest && (
          <span data-tour={tourAnchor ? 'consenso' : undefined}>
            <Consensus readings={latest.readings} />
          </span>
        )}

        <svg
          className={`chevron ${expanded ? 'chevron--open' : ''}`}
          width="16"
          height="16"
          viewBox="0 0 16 16"
          fill="none"
          aria-hidden="true"
        >
          <path d="M4 6l4 4 4-4" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
      </button>

      {expanded && (
        <div className="card__body" id={panelId}>
          {company.documents.map((document, i) => (
            <DocumentBlock
              key={document.documentId}
              document={document}
              tourAnchor={tourAnchor && i === 0}
            />
          ))}
        </div>
      )}
    </div>
  );
}
