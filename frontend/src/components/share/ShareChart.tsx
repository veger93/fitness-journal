import { useEffect, useState } from 'react';
import { renderShareImage, type ShareChartData, type ShareFormat } from '../../lib/shareImage';
import { Button } from '../ui/Button';
import { Dialog } from '../ui/Dialog';
import { SegmentedControl } from '../ui/SegmentedControl';
import { Switch } from '../ui/Switch';
import styles from './Share.module.css';

const FORMATS: { value: ShareFormat; label: string }[] = [
  { value: 'POST', label: 'Пост 4:5' },
  { value: 'STORY', label: 'История 9:16' },
];

type Props = Omit<ShareChartData, 'showWeight' | 'format'>;

/**
 * Значок "поделиться" в шапке карточки графика. По нажатию — окно с готовой картинкой:
 * формат (пост / история), "показывать вес", и системное меню телефона "Поделиться" (Telegram, WhatsApp, фото).
 * Картинку рисуем в браузере (lib/shareImage): на сервер ничего не уходит.
 */
export function ShareChartButton(props: Props) {
  const [open, setOpen] = useState(false);
  return (
    <>
      <button
        type="button"
        className={styles.iconButton}
        aria-label={`Поделиться графиком: ${props.exerciseName}`}
        onClick={() => setOpen(true)}
      >
        <svg
          aria-hidden="true"
          width="20"
          height="20"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
        >
          <path d="M12 3v12M7 8l5-5 5 5" />
          <path d="M5 13v6a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2v-6" />
        </svg>
      </button>
      {open && <ShareDialog {...props} onClose={() => setOpen(false)} />}
    </>
  );
}

function ShareDialog({ onClose, ...data }: Props & { onClose: () => void }) {
  const [format, setFormat] = useState<ShareFormat>('POST');
  const [showWeight, setShowWeight] = useState(true);
  const [image, setImage] = useState<{ blob: Blob; url: string } | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [sending, setSending] = useState(false);
  // данные графика сравниваем по значению: новый массив points на каждый рендер не должен перерисовывать картинку
  const key = JSON.stringify(data);

  useEffect(() => {
    let cancelled = false;
    let url: string | null = null;
    renderShareImage({ ...(JSON.parse(key) as Props), showWeight, format })
      .then((blob) => {
        if (cancelled) {
          return;
        }
        url = URL.createObjectURL(blob);
        setImage({ blob, url });
        setError(null);
      })
      .catch((e: Error) => !cancelled && setError(e.message));
    // старую картинку освобождаем — иначе каждый переключатель оставлял бы в памяти ещё один PNG
    return () => {
      cancelled = true;
      if (url) {
        URL.revokeObjectURL(url);
      }
    };
  }, [key, showWeight, format]);

  const fileName = `vegas-${data.exerciseName.toLowerCase().replace(/[^a-zа-яё0-9]+/gi, '-')}.png`;
  const file = image ? new File([image.blob], fileName, { type: 'image/png' }) : null;
  // телефон умеет отправлять файлы — системное меню; иначе (большинство компьютеров) — просто скачать
  const canShare = file !== null && typeof navigator.canShare === 'function' && navigator.canShare({ files: [file] });

  const send = async () => {
    if (!file || !image) {
      return;
    }
    if (!canShare) {
      download(image.url, fileName);
      return;
    }
    setSending(true);
    try {
      await navigator.share({ files: [file], title: data.exerciseName });
    } catch (e) {
      // закрыл меню "Поделиться" — это не ошибка
      if (!(e instanceof DOMException && e.name === 'AbortError')) {
        setError('Не получилось отправить — сохраните картинку и отправьте вручную.');
      }
    } finally {
      setSending(false);
    }
  };

  return (
    <Dialog open onClose={onClose} title="Поделиться прогрессом">
      <div className={styles.body}>
        <div className={`${styles.preview} ${format === 'STORY' ? styles.story : ''}`}>
          {image ? <img src={image.url} alt={`Картинка: ${data.exerciseName}`} /> : <span>Рисуем…</span>}
        </div>
        <SegmentedControl legend="Формат" options={FORMATS} value={format} onChange={setFormat} />
        <Switch
          label="Показывать вес в кг"
          hint={showWeight ? undefined : 'Только рост силы в процентах'}
          checked={showWeight}
          onChange={setShowWeight}
        />
        {error && <p className={styles.error}>{error}</p>}
        <Button fullWidth onClick={send} disabled={!image || sending}>
          {canShare ? 'Отправить…' : 'Скачать картинку'}
        </Button>
      </div>
    </Dialog>
  );
}

function download(url: string, fileName: string) {
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName;
  link.click();
}
