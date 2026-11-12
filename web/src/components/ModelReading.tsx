import { useState } from 'react';
import type { DocumentCard, ModelReading as Reading } from '../types';
import { MODEL_LABELS } from '../types';
import {
  documentLabel,
  eventLabel,
  formatDate,
  riskTone,
  safeExternalUrl,
  sentimentTone,
} from '../lib/format';
import { Chip, Confidence } from './Chip';

/** Um trecho citado, com o sinal que ele sustenta. */
interface Citation {
  origin: string;
  text: string;
}

/**
 * Reúne os trechos citados por este modelo, cada um rotulado com o sinal
 * que sustenta. Trechos repetidos são colapsados — quando o modelo lista o
 * mesmo evento duas vezes, ele costuma citar a mesma frase nas duas.
 */
function citationsOf(reading: Reading): Citation[] {
  const candidates: Citation[] = [];
  if (reading.sentiment?.evidence?.trim()) {
    candidates.push({ origin: 'Sentimento', text: reading.sentiment.evidence.trim() });
  }
  if (reading.risk?.evidence?.trim()) {
    candidates.push({ origin: 'Risco', text: reading.risk.evidence.trim() });
  }
  for (const event of reading.events) {
    if (event.evidence?.trim()) {
      candidates.push({ origin: `Evento: ${eventLabel(event.label)}`, text: event.evidence.trim() });
    }
  }

  const seen = new Set<string>();
  return candidates.filter((c) => (seen.has(c.text) ? false : seen.add(c.text)));
}

/**
 * A leitura de um LLM sobre um documento: sentimento, risco e eventos.
 *
 * O pipeline não emite recomendação de compra ou venda — o que o modelo
 * devolve é esta classificação, e é ela que o participante avalia.
 */
export function ModelReadingCard({
  reading,
  document,
  tourAnchor = false,
}: {
  reading: Reading;
  /** O documento de onde saíram os trechos — a origem exibida ao expandir. */
  document: DocumentCard;
  tourAnchor?: boolean;
}) {
  const [showEvidence, setShowEvidence] = useState(false);
  const { sentiment, risk, events } = reading;
  const isEmpty = !sentiment && !risk && events.length === 0;

  const citations = citationsOf(reading);

  return (
    <article className="reading">
      <h4 className="reading__model">
        <span className="reading__badge" aria-hidden="true" />
        {MODEL_LABELS[reading.model] ?? reading.model}
      </h4>

      {isEmpty ? (
        <p className="reading__empty">Este modelo não retornou sinais para o documento.</p>
      ) : (
        <>
          <div className="signal">
            <span className="signal__label">Sentimento</span>
            <div className="signal__row">
              {sentiment ? (
                <>
                  <Chip tone={sentimentTone(sentiment.label)}>{sentiment.label}</Chip>
                  <Confidence value={sentiment.confidence} />
                </>
              ) : (
                <span className="reading__empty">não informado</span>
              )}
            </div>
          </div>

          <div className="signal">
            <span className="signal__label">Risco</span>
            <div className="signal__row">
              {risk ? (
                <>
                  <Chip tone={riskTone(risk.label)}>{risk.label}</Chip>
                  <Confidence value={risk.confidence} />
                </>
              ) : (
                <span className="reading__empty">não informado</span>
              )}
            </div>
          </div>

          <div className="signal">
            <span className="signal__label">Eventos corporativos</span>
            {events.length === 0 ? (
              <span className="reading__empty">nenhum identificado</span>
            ) : (
              <div className="events">
                {events.map((event, i) => (
                  <Chip key={`${event.label}-${i}`} tone="info">
                    {eventLabel(event.label)}
                  </Chip>
                ))}
              </div>
            )}
          </div>

          {sentiment?.justification && <p className="justification">{sentiment.justification}</p>}

          {citations.length > 0 && (
            <>
              <button
                type="button"
                className="details__toggle"
                data-tour={tourAnchor ? 'evidencia' : undefined}
                aria-expanded={showEvidence}
                onClick={() => setShowEvidence((open) => !open)}
              >
                {showEvidence ? 'Ocultar trechos citados' : 'Ver trechos citados do documento'}
              </button>

              {showEvidence && (
                <div className="citations">
                  <p className="citations__source">
                    <span className="citations__label">Fonte</span>
                    {documentLabel(document.type)} de {formatDate(document.referenceDate)}
                    {document.protocol && ` · protocolo ${document.protocol}`}
                    {safeExternalUrl(document.sourceUrl) && (
                      <>
                        {' · '}
                        <a
                          href={safeExternalUrl(document.sourceUrl)!}
                          target="_blank"
                          rel="noopener noreferrer"
                        >
                          abrir o documento na CVM
                        </a>
                      </>
                    )}
                  </p>

                  {citations.map((citation, i) => (
                    <figure className="citation" key={i}>
                      <figcaption className="citation__origin">{citation.origin}</figcaption>
                      <blockquote className="evidence">“{citation.text}”</blockquote>
                    </figure>
                  ))}
                </div>
              )}
            </>
          )}
        </>
      )}
    </article>
  );
}
