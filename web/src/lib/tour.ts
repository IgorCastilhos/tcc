/**
 * Tour de primeiro acesso. Mostrado uma única vez por navegador; depois
 * disso, só sob demanda pelo botão de ajuda.
 *
 * A chave carrega a versão: mudando os passos de forma relevante, basta
 * subir o número para que quem já viu o tour antigo veja o novo.
 */
const STORAGE_KEY = 'cvm-signals:tour:v1';

export interface TourStep {
  /** Valor do atributo data-tour do elemento destacado. Ausente = balão centralizado. */
  target?: string;
  title: string;
  body: string;
  /** Abre o primeiro card antes de exibir o passo, para o destaque ter o que mostrar. */
  expandsFirstCard?: boolean;
  /** Passo pulado quando ainda não há empresa no painel. */
  requiresCompany?: boolean;
}

export const TOUR_STEPS: TourStep[] = [
  {
    title: 'Bem-vindo ao painel de sinais',
    body:
      'Três inteligências artificiais — Claude Sonnet, GPT-4o e Gemma — leem os mesmos documentos '
      + 'oficiais da CVM e dizem o que enxergam em cada um. Em 30 segundos você entende como ler a tela.',
  },
  {
    target: 'busca',
    title: 'Encontre uma empresa',
    body:
      'Busque pelo nome, pelo código da ação na B3 (ex.: PETR4) ou pelo CNPJ. '
      + 'Deixe em branco para ver todas as empresas analisadas.',
  },
  {
    target: 'identidade',
    title: 'Quem é a empresa',
    body:
      'Cada linha é uma companhia: o código da ação, a razão social, o CNPJ e quantos '
      + 'documentos regulatórios já foram analisados por ela.',
    requiresCompany: true,
  },
  {
    target: 'cotacao',
    title: 'Quanto vale a ação hoje',
    body:
      'O último preço de fechamento na B3 e a variação em relação ao pregão anterior. '
      + 'O gráfico ao lado mostra o comportamento dos últimos pregões.',
    requiresCompany: true,
  },
  {
    target: 'consenso',
    title: 'Os três modelos de relance',
    body:
      'Dois pontos por modelo: o primeiro é o sentimento, o segundo é o risco. '
      + 'Verde é favorável, amarelo é intermediário, vermelho é desfavorável. '
      + 'Quando os pontos divergem, os modelos discordaram — e isso é justamente o que o estudo investiga.',
    requiresCompany: true,
  },
  {
    target: 'leituras',
    title: 'A leitura completa, lado a lado',
    body:
      'Clique em qualquer linha para abrir. Cada coluna traz o que um modelo concluiu: sentimento, '
      + 'nível de risco, eventos corporativos encontrados e o quanto ele mesmo diz estar confiante.',
    expandsFirstCard: true,
    requiresCompany: true,
  },
  {
    target: 'evidencia',
    title: 'Confira a fonte',
    body:
      'Em "Ver trechos citados do documento" aparece o texto original em que o modelo se apoiou. '
      + 'Use isso para julgar se a conclusão faz sentido — é o ponto central da sua avaliação.',
    expandsFirstCard: true,
    requiresCompany: true,
  },
  {
    target: 'aba-questionario',
    title: 'O que você deve fazer',
    body:
      'Ao final de cada semana do estudo, abra o Questionário e responda ao formulário sobre a sua '
      + 'experiência. É a sua percepção que está sendo avaliada, não a sua capacidade de acertar o mercado.',
  },
  {
    target: 'aviso',
    title: 'Nada aqui é recomendação',
    body:
      'Os sinais são informativos e gerados automaticamente. A ferramenta não sugere comprar nem '
      + 'vender nada, e sua participação é voluntária, conforme o termo de consentimento.',
  },
];

export function hasSeenTour(): boolean {
  try {
    return localStorage.getItem(STORAGE_KEY) === 'done';
  } catch {
    // Navegador com armazenamento bloqueado: melhor não mostrar o tour
    // repetidamente do que insistir a cada carregamento.
    return true;
  }
}

export function markTourSeen(): void {
  try {
    localStorage.setItem(STORAGE_KEY, 'done');
  } catch {
    // Sem armazenamento não há o que registrar; o tour simplesmente fecha.
  }
}
