import { useState } from 'react';
import type { Submission } from '../types';
import { submitEvaluation } from '../lib/api';

const LIKERT = [1, 2, 3, 4, 5];
const TOTAL = 10;

/** As afirmações 1–5 medem facilidade de uso; 6–10, utilidade percebida (Quadro 4). */
const DIMENSIONS = [
  { label: 'Facilidade de uso percebida', from: 1, to: 5 },
  { label: 'Utilidade percebida', from: 6, to: 10 },
];

type Status =
  | { kind: 'idle' }
  | { kind: 'sending' }
  | { kind: 'sent' }
  | { kind: 'offline'; payload: Submission };

export function Questionnaire({ statements }: { statements: string[] }) {
  const [participantCode, setParticipantCode] = useState('');
  const [week, setWeek] = useState(1);
  const [answers, setAnswers] = useState<Record<number, number>>({});
  const [comments, setComments] = useState('');
  const [status, setStatus] = useState<Status>({ kind: 'idle' });
  const [showErrors, setShowErrors] = useState(false);

  const answered = Object.keys(answers).length;
  const missingCode = participantCode.trim() === '';
  const complete = answered === TOTAL && !missingCode;

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    if (!complete) {
      setShowErrors(true);
      return;
    }
    setStatus({ kind: 'sending' });
    const submission: Submission = {
      participantCode: participantCode.trim(),
      week,
      answers,
      comments: comments.trim(),
    };
    const result = await submitEvaluation(submission);
    if (result.kind === 'sent') {
      setStatus({ kind: 'sent' });
      setAnswers({});
      setComments('');
      setShowErrors(false);
    } else {
      setStatus({ kind: 'offline', payload: result.payload });
    }
  }

  function download(payload: Submission) {
    const blob = new Blob([JSON.stringify(payload, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `tam-${payload.participantCode}-semana${payload.week}.json`;
    link.click();
    URL.revokeObjectURL(url);
  }

  return (
    <form className="panel" onSubmit={handleSubmit}>
      <h2 className="panel__title">Questionário semanal</h2>
      <p className="panel__hint">
        Instrumento TAM (Davis, 1989) — Quadro 4 da monografia. Responda ao fim de cada semana do
        estudo, depois de consultar os sinais. Participação voluntária, conforme o TCLE.
      </p>

      <div className="grid-2">
        <div className="field">
          <label htmlFor="participante">Código do participante</label>
          <input
            id="participante"
            value={participantCode}
            onChange={(e) => setParticipantCode(e.target.value)}
            placeholder="ex.: P03"
            maxLength={40}
            autoComplete="off"
          />
          {showErrors && missingCode && (
            <span className="field__error">Informe o código pseudonimizado que você recebeu.</span>
          )}
        </div>
        <div className="field">
          <label htmlFor="semana">Semana do estudo</label>
          <select id="semana" value={week} onChange={(e) => setWeek(Number(e.target.value))}>
            <option value={1}>Semana 1</option>
            <option value={2}>Semana 2</option>
            <option value={3}>Semana 3</option>
          </select>
        </div>
      </div>

      {DIMENSIONS.map((dimension) => (
        <fieldset key={dimension.label} style={{ border: 0, padding: 0, margin: 0 }}>
          <legend className="section-label">{dimension.label}</legend>
          {statements.slice(dimension.from - 1, dimension.to).map((text, offset) => {
            const number = dimension.from + offset;
            return (
              <div className="statement" key={number}>
                <p className="statement__text">
                  <span className="statement__num">{number}.</span>
                  <span>{text}</span>
                </p>
                <div className="likert" role="radiogroup" aria-label={`Afirmação ${number}: ${text}`}>
                  {LIKERT.map((value) => (
                    <label className="likert__option" key={value}>
                      <input
                        type="radio"
                        name={`afirmacao-${number}`}
                        value={value}
                        checked={answers[number] === value}
                        onChange={() => setAnswers((prev) => ({ ...prev, [number]: value }))}
                      />
                      <span>{value}</span>
                    </label>
                  ))}
                </div>
              </div>
            );
          })}
          <div className="scale-legend">
            <span>1 — discordo totalmente</span>
            <span>5 — concordo totalmente</span>
          </div>
        </fieldset>
      ))}

      <div className="field" style={{ marginTop: 24 }}>
        <label htmlFor="comentario">
          Comentários (opcional) — descreva livremente sua percepção sobre os sinais gerados
        </label>
        <textarea
          id="comentario"
          rows={4}
          value={comments}
          onChange={(e) => setComments(e.target.value)}
        />
      </div>

      <div className="actions">
        <button className="btn" type="submit" disabled={status.kind === 'sending'}>
          {status.kind === 'sending' ? 'Enviando…' : 'Enviar respostas'}
        </button>

        <span className="feedback">
          {showErrors && answered < TOTAL && (
            <span className="feedback--erro">
              Faltam {TOTAL - answered} {TOTAL - answered === 1 ? 'afirmação' : 'afirmações'}.
            </span>
          )}
          {status.kind === 'sent' && (
            <span className="feedback--ok">Respostas registradas. Obrigado!</span>
          )}
          {status.kind === 'idle' && !showErrors && answered > 0 && answered < TOTAL && (
            <span style={{ color: 'var(--text-muted)' }}>
              {answered} de {TOTAL} respondidas
            </span>
          )}
        </span>
      </div>

      {status.kind === 'offline' && (
        <div className="disclaimer" style={{ marginTop: 18, marginBottom: 0 }}>
          <div>
            <strong>Não foi possível enviar automaticamente.</strong> Esta versão do site está
            publicada sem servidor de coleta. Salve o arquivo com suas respostas e envie ao
            pesquisador — nada foi perdido.
            <div className="actions" style={{ marginTop: 12 }}>
              <button type="button" className="btn btn--ghost" onClick={() => download(status.payload)}>
                Salvar respostas (JSON)
              </button>
              <button
                type="button"
                className="btn btn--ghost"
                onClick={() => navigator.clipboard?.writeText(JSON.stringify(status.payload, null, 2))}
              >
                Copiar
              </button>
            </div>
          </div>
        </div>
      )}
    </form>
  );
}
