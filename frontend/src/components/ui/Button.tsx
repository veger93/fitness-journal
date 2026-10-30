import type { ButtonHTMLAttributes, Ref } from 'react';
import { Link, type LinkProps } from 'react-router-dom';
import styles from './Button.module.css';

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'surface' | 'accent';
  fullWidth?: boolean;
  /** React 19: ref — обычный проп (окно может "вырасти" из кнопки, см. Dialog originRef) */
  ref?: Ref<HTMLButtonElement>;
}

/** Единственная кнопка приложения: несколько разновидностей, форма-"пилюля", высота 52px. */
export function Button({ variant = 'primary', fullWidth = false, className, type = 'button', ...rest }: ButtonProps) {
  const classes = [styles.button, styles[variant], fullWidth && styles.fullWidth, className].filter(Boolean).join(' ');
  return <button type={type} className={classes} {...rest} />;
}

interface ButtonLinkProps extends LinkProps {
  variant?: 'primary' | 'secondary' | 'surface' | 'accent';
  fullWidth?: boolean;
}

/** Переход на другую страницу, оформленный как кнопка. Семантически это ссылка — так правильно для доступности. */
export function ButtonLink({ variant = 'primary', fullWidth = false, className, ...rest }: ButtonLinkProps) {
  const classes = [styles.button, styles[variant], fullWidth && styles.fullWidth, className].filter(Boolean).join(' ');
  return <Link className={classes} {...rest} />;
}
