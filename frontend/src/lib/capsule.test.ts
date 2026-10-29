import { describe, expect, it } from 'vitest';
import { capsuleFrame } from './capsule';

const home = { left: 0, right: 100 };
const records = { left: 200, right: 260 };

describe('capsuleFrame', () => {
  it('в начале стоит на старой вкладке, в конце — ровно на новой', () => {
    expect(capsuleFrame(home, records, 0)).toEqual({ ...home, squash: 0 });
    expect(capsuleFrame(home, records, 1)).toEqual({ ...records, squash: 0 });
  });

  it('при движении вправо передний (правый) край убегает вперёд, задний отстаёт — капля вытягивается', () => {
    const mid = capsuleFrame(home, records, 0.3);
    const rightProgress = (mid.right - home.right) / (records.right - home.right);
    const leftProgress = (mid.left - home.left) / (records.left - home.left);
    expect(rightProgress).toBeGreaterThan(leftProgress);
    expect(mid.right - mid.left).toBeGreaterThan(records.right - records.left);
    expect(mid.squash).toBeGreaterThan(0);
  });

  it('при движении влево вперёд убегает левый край', () => {
    const mid = capsuleFrame(records, home, 0.3);
    const leftProgress = (records.left - mid.left) / (records.left - home.left);
    const rightProgress = (records.right - mid.right) / (records.right - home.right);
    expect(leftProgress).toBeGreaterThan(rightProgress);
  });

  it('сплющивание ограничено, чтобы капсула не превращалась в нитку', () => {
    const far = capsuleFrame({ left: 0, right: 40 }, { left: 1000, right: 1040 }, 0.4);
    expect(far.squash).toBeLessThanOrEqual(0.12);
  });
});
