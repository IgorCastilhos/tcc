import { useCallback, useEffect, useLayoutEffect, useRef, useState } from 'react';
import type { TourStep } from '../lib/tour';

interface Props {
  steps: TourStep[];
  onFinish: () => void;
  /** Avisa o App quando um passo precisa do primeiro card aberto. */
  onExpandFirstCard: (expand: boolean) => void;
}

interface Placement {
  /** Recorte iluminado sobre o elemento em destaque. */
  spotlight: { top: number; left: number; width: number; height: number } | null;
  balloon: { top: number; left: number };
  arrow: 'up' | 'down' | null;
}

const BALLOON_WIDTH = 340;
const GAP = 14;
const MARGIN = 12;

/**
 * Sobreposição com recorte no elemento do passo e um balão explicativo.
 *
 * O recorte é feito com um box-shadow gigantesco em volta de uma caixa
 * posicionada sobre o alvo: escurece a página inteira menos aquele
 * retângulo, sem precisar de máscara SVG nem de clonar o elemento.
 */
export function Tour({ steps, onFinish, onExpandFirstCard }: Props) {
  const [index, setIndex] = useState(0);
  const [placement, setPlacement] = useState<Placement | null>(null);
  const balloonRef = useRef<HTMLDivElement>(null);

  const step = steps[index];
  const isLast = index === steps.length - 1;

  // Avisa o App antes de medir: o card precisa estar aberto para o alvo existir.
  useEffect(() => {
    onExpandFirstCard(Boolean(step?.expandsFirstCard));
  }, [step, onExpandFirstCard]);

  const reposition = useCallback(() => {
    if (!step) return;

    if (!step.target) {
      setPlacement({
        spotlight: null,
        balloon: {
          top: Math.round(window.innerHeight / 2 - 120),
          left: Math.round(window.innerWidth / 2 - BALLOON_WIDTH / 2),
        },
        arrow: null,
      });
      return;
    }

    const element = document.querySelector<HTMLElement>(`[data-tour="${step.target}"]`);
    if (!element) {
      setPlacement(null);
      return;
    }

    const rect = element.getBoundingClientRect();
    const pad = 6;
    const spotlight = {
      top: rect.top - pad,
      left: rect.left - pad,
      width: rect.width + pad * 2,
      height: rect.height + pad * 2,
    };

    const balloonHeight = balloonRef.current?.offsetHeight ?? 190;
    const below = spotlight.top + spotlight.height + GAP;
    const fitsBelow = below + balloonHeight < window.innerHeight - MARGIN;
    const top = fitsBelow ? below : spotlight.top - balloonHeight - GAP;

    const centered = rect.left + rect.width / 2 - BALLOON_WIDTH / 2;
    const left = Math.min(
      Math.max(MARGIN, centered),
      window.innerWidth - BALLOON_WIDTH - MARGIN,
    );

    setPlacement({
      spotlight,
      balloon: { top: Math.max(MARGIN, top), left },
      arrow: fitsBelow ? 'up' : 'down',
    });
  }, [step]);

  // Rola o alvo para a área visível antes de medir, senão o recorte
  // aparece fora da tela quando o passo está mais abaixo na página.
  useLayoutEffect(() => {
    if (!step?.target) {
      reposition();
      return;
    }
    const element = document.querySelector<HTMLElement>(`[data-tour="${step.target}"]`);
    element?.scrollIntoView({ block: 'center', behavior: 'smooth' });
    const timer = window.setTimeout(reposition, 320);
    return () => window.clearTimeout(timer);
  }, [step, reposition]);

  useEffect(() => {
    window.addEventListener('resize', reposition);
    window.addEventListener('scroll', reposition, true);
    return () => {
      window.removeEventListener('resize', reposition);
      window.removeEventListener('scroll', reposition, true);
    };
  }, [reposition]);

  const finish = useCallback(() => {
    onExpandFirstCard(false);
    onFinish();
  }, [onExpandFirstCard, onFinish]);

  const next = useCallback(() => {
    if (isLast) finish();
    else setIndex((i) => i + 1);
  }, [isLast, finish]);

  const previous = useCallback(() => setIndex((i) => Math.max(0, i - 1)), []);

  useEffect(() => {
    function onKey(event: KeyboardEvent) {
      if (event.key === 'Escape') finish();
      if (event.key === 'ArrowRight') next();
      if (event.key === 'ArrowLeft') previous();
    }
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [finish, next, previous]);

  useEffect(() => {
    balloonRef.current?.focus();
  }, [index]);

  if (!step) return null;

  return (
    <div className="tour" role="dialog" aria-modal="true" aria-labelledby="tour-title">
      <div className="tour__backdrop" onClick={finish} />

      {placement?.spotlight && (
        <div
          className="tour__spotlight"
          style={{
            top: placement.spotlight.top,
            left: placement.spotlight.left,
            width: placement.spotlight.width,
            height: placement.spotlight.height,
          }}
        />
      )}

      <div
        ref={balloonRef}
        className={`tour__balloon ${placement?.arrow ? `tour__balloon--${placement.arrow}` : ''}`}
        style={{
          top: placement?.balloon.top ?? window.innerHeight / 2,
          left: placement?.balloon.left ?? MARGIN,
        }}
        tabIndex={-1}
      >
        <div className="tour__count">
          Passo {index + 1} de {steps.length}
        </div>
        <h2 className="tour__title" id="tour-title">
          {step.title}
        </h2>
        <p className="tour__body">{step.body}</p>

        <div className="tour__dots" aria-hidden="true">
          {steps.map((_, i) => (
            <span key={i} className={`tour__dot ${i === index ? 'tour__dot--on' : ''}`} />
          ))}
        </div>

        <div className="tour__actions">
          <button type="button" className="tour__skip" onClick={finish}>
            {isLast ? 'Fechar' : 'Pular apresentação'}
          </button>
          <div className="tour__nav">
            {index > 0 && (
              <button type="button" className="btn btn--ghost" onClick={previous}>
                Anterior
              </button>
            )}
            <button type="button" className="btn" onClick={next}>
              {isLast ? 'Começar a usar' : 'Próximo'}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
