import { useMutation } from '@tanstack/react-query';
import QRCode from 'qrcode';
import { useEffect, useState } from 'react';
import { trainerApi } from '../../api/trainer';
import { formatShortDate } from '../../lib/format';
import { Button } from '../ui/Button';
import { Card } from '../ui/Card';
import { Dialog } from '../ui/Dialog';
import styles from './Trainer.module.css';

/**
 * Тренер: "Передать аккаунт" клиенту (docs/trainer.md, шаг 4).
 * В карточке — пояснение и кнопка; QR и ссылка — во всплывающем окне (мягко всплывает снизу, по центру, с крестиком):
 * в зале клиент просто сканирует QR камерой своего телефона.
 * Ссылка одноразовая, на 7 дней. "Новая ссылка" отменяет старую — если QR кто-то сфотографировал.
 */
export function HandoverCard({ clientId, clientName }: { clientId: string; clientName: string }) {
  const [open, setOpen] = useState(false);
  const [copied, setCopied] = useState(false);
  const [qr, setQr] = useState<string | null>(null);
  const create = useMutation({ mutationFn: () => trainerApi.createClaimLink(clientId) });
  const url = create.data ? `${window.location.origin}/claim/${create.data.token}` : null;

  // QR рисуем в SVG-строку: чёткий на любом экране, без canvas
  useEffect(() => {
    let cancelled = false;
    if (url) {
      QRCode.toString(url, { type: 'svg', margin: 1, errorCorrectionLevel: 'M' }).then((svg) => {
        if (!cancelled) setQr(svg);
      });
    }
    return () => {
      cancelled = true;
    };
  }, [url]);

  const openDialog = () => {
    setOpen(true);
    // ссылку создаём при первом открытии; созданная действует, пока не нажали "Новая ссылка"
    if (!create.data && !create.isPending) create.mutate();
  };

  const copy = async () => {
    if (!url) return;
    try {
      await navigator.clipboard.writeText(url);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      // буфер обмена недоступен (старый браузер) — ссылку видно в окне, её можно выделить
    }
  };

  return (
    <Card>
      <h2 className={styles.cardTitle}>Передать аккаунт</h2>
      <p className={styles.muted}>
        {clientName} получит свой вход: все тренировки, рекорды и графики останутся с ним. Он сам решит — продолжать с
        вами или дальше самостоятельно.
      </p>
      <Button variant="accent" onClick={openDialog}>
        Показать QR-код
      </Button>

      <Dialog open={open} onClose={() => setOpen(false)} title="Передать аккаунт">
        <div className={styles.handover}>
          <p className={styles.muted}>Пусть {clientName} отсканирует код камерой телефона или откроет ссылку.</p>
          {create.isError && <p className={styles.error}>{create.error.message}</p>}
          {qr && url ? (
            // SVG от библиотеки qrcode — наша же строка, не пользовательский ввод
            <div className={styles.qr} role="img" aria-label="QR-код ссылки" dangerouslySetInnerHTML={{ __html: qr }} />
          ) : (
            !create.isError && <div className={styles.qrPlaceholder} aria-label="Создаём ссылку…" />
          )}
          {url && <p className={styles.link}>{url}</p>}
          <div className={styles.handoverActions}>
            <Button variant="secondary" fullWidth onClick={copy} disabled={!url}>
              {copied ? 'Скопировано ✓' : 'Скопировать ссылку'}
            </Button>
            <Button variant="secondary" fullWidth onClick={() => create.mutate()} disabled={create.isPending}>
              Новая ссылка
            </Button>
          </div>
          {create.data && (
            <p className="footnote">
              Действует до {formatShortDate(create.data.expiresAt)}, сработает один раз. Новая ссылка отменяет старую.
            </p>
          )}
        </div>
      </Dialog>
    </Card>
  );
}
