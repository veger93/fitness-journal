import { describe, expect, it } from 'vitest';
import { describeElement, findOffenders, isBroken } from './layoutProbe';

const base = { clientWidth: 390, scrollWidth: 390, scrollX: 0, vvScale: 1, vvOffsetLeft: 0 };

describe('диагностика вёрстки', () => {
  it('целая страница — не поломка', () => {
    expect(isBroken(base)).toBe(false);
    expect(isBroken({ ...base, vvScale: null, vvOffsetLeft: null })).toBe(false);
  });

  it('шире экрана, сдвиг вбок или масштаб не 1 — поломка', () => {
    expect(isBroken({ ...base, scrollWidth: 470 })).toBe(true);
    expect(isBroken({ ...base, scrollX: 80 })).toBe(true);
    expect(isBroken({ ...base, vvScale: 0.84 })).toBe(true);
    expect(isBroken({ ...base, vvOffsetLeft: 40 })).toBe(true);
    expect(isBroken({ ...base, offenders: [{ name: 'div', left: 0, right: 470, width: 470 }] })).toBe(true);
  });

  it('имя элемента — тег и классы', () => {
    const el = document.createElement('div');
    el.className = 'ui-card _card_x7f3a';
    expect(describeElement(el)).toBe('div.ui-card._card_x7f3a');
    expect(describeElement(document.createElement('span'))).toBe('span');
  });

  it('за краем — только то, что выступает и не внутри ленты с прокруткой', () => {
    document.body.innerHTML = '<div id="wide"></div><div style="overflow-x:auto"><div id="chip"></div></div>';
    const rect = (left: number, right: number) => ({ left, right, width: right - left, height: 10 }) as DOMRect;
    document.getElementById('wide')!.getBoundingClientRect = () => rect(0, 470);
    document.getElementById('chip')!.getBoundingClientRect = () => rect(300, 600);
    const found = findOffenders(document.body, 390);
    expect(found.map((o) => o.name)).toEqual(['div']);
    expect(found[0]).toMatchObject({ right: 470, width: 470 });
  });
});
