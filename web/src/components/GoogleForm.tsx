/**
 * Questionário TAM hospedado no Google Forms.
 *
 * O instrumento do Quadro 4 saiu da aplicação e passou a ser respondido
 * aqui. Isso resolve a coleta no site publicado com dados congelados, que
 * não tem backend para receber um POST — as respostas vão direto para a
 * planilha do formulário.
 */

/** URL do formulário embutido. Trocar o formulário é trocar só esta constante. */
const FORM_ID = '1FAIpQLScypE8FhpL3R5NxP9cUDEL_batC7BX2F3VmGxMDMUJudFbA2A';
const EMBED_URL = `https://docs.google.com/forms/d/e/${FORM_ID}/viewform?embedded=true`;
const OPEN_URL = `https://docs.google.com/forms/d/e/${FORM_ID}/viewform`;

/** Altura que o Google gerou para este formulário. */
const FORM_HEIGHT = 2243;

export function GoogleForm() {
  return (
    <section className="panel">
      <h2 className="panel__title">Questionário semanal</h2>
      <p className="panel__hint">
        Instrumento TAM (Davis, 1989) — Quadro 4 da monografia. Responda ao fim de cada semana do
        estudo, depois de consultar os sinais. Participação voluntária, conforme o TCLE.{' '}
        <a href={OPEN_URL} target="_blank" rel="noopener noreferrer">
          Se o formulário não carregar, abra em uma nova aba.
        </a>
      </p>

      <div className="formwrap">
        <iframe
          src={EMBED_URL}
          title="Questionário de utilidade percebida (TAM)"
          height={FORM_HEIGHT}
          frameBorder="0"
          marginHeight={0}
          marginWidth={0}
        >
          Carregando…
        </iframe>
      </div>
    </section>
  );
}
