import { useMutation } from '@tanstack/react-query';
import QRCode from 'qrcode';
import { useEffect, useState } from 'react';
import { trainerApi } from '../../api/trainer';
import { formatShortDate } from '../../lib/format';
import { Button } from '../ui/Button';
import { Card } from '../ui/Card';
import styles from './Trainer.module.css';

/**
 * Тренер: "Передать аккаунт" клиенту (docs/trainer.md, шаг 4).
 * Одноразовая ссылка на 7 дней + QR-код: в зале клиент просто сканирует его камерой своего телефона.
 * Новая ссылка отменяет старую — если QR кто-то сфотографировал, достаточно нажать "Новая ссылка".
 */
export function HandoverCard({ clientId, clientName }: { clientId: string; clientName: string }) {
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

  const copy = async () => {
    if (!url) return;
    try {
      await navigator.clipboard.writeText(url);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      // буфер обмена недоступен (старый браузер) — ссылку видно на экране, её можно выделить
    }
  };

  return (
    <Card>
      <h2 className={styles.cardTitle}>Передать аккаунт</h2>
      {!create.data ? (
        <>
          <p className={styles.muted}>
            {clientName} получит свой вход: все тренировки, рекорды и графики останутся с ним. Он сам решит — продолжать
            с вами или дальше самостоятельно.
          </p>
          {create.isError && <p className={styles.error}>{create.error.message}</p>}
          <Button variant="accent" onClick={() => create.mutate()} disabled={create.isPending}>
            Создать ссылку и QR-код
          </Button>
        </>
      ) : (
        <>
          <p className={styles.muted}>Пусть {clientName} отсканирует код камерой телефона или откроет ссылку.</p>
          {qr && (
            // SVG от библиотеки qrcode — наша же строка, не пользовательский ввод
            <div className={styles.qr} role="img" aria-label="QR-код ссылки" dangerouslySetInnerHTML={{ __html: qr }} />
          )}
          <p className={styles.link}>{url}</p>
          <div className={styles.actions}>
            <Button variant="secondary" onClick={copy}>
              {copied ? 'Скопировано ✓' : 'Скопировать ссылку'}
            </Button>
            <Button variant="secondary" onClick={() => create.mutate()} disabled={create.isPending}>
              Новая ссылка
            </Button>
          </div>
          <p className="footnote">
            Действует до {formatShortDate(create.data.expiresAt)}, сработает один раз. Новая ссылка отменяет старую.
          </p>
        </>
      )}
    </Card>
  );
}
