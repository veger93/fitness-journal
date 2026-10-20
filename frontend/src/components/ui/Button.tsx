import type { ButtonHTMLAttributes } from 'react';
import { Link, type LinkProps } from 'react-router-dom';
import styles from './Button.module.css';

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'surface';
  fullWidth?: boolean;
}

/** Единственная кнопка приложения: две разновидности, форма-"пилюля", высота 52px. */
export function Button({ variant = 'primary', fullWidth = false, className, type = 'button', ...rest }: ButtonProps) {
  const classes = [styles.button, styles[variant], fullWidth && styles.fullWidth, className].filter(Boolean).join(' ');
  return <button type={type} className={classes} {...rest} />;
}

interface ButtonLinkProps extends LinkProps {
  variant?: 'primary' | 'secondary' | 'surface';
  fullWidth?: boolean;
}

/** Переход на другую страницу, оформленный как кнопка. Семантически это ссылка — так правильно для доступности. */
export function ButtonLink({ variant = 'primary', fullWidth = false, className, ...rest }: ButtonLinkProps) {
  const classes = [styles.button, styles[variant], fullWidth && styles.fullWidth, className].filter(Boolean).join(' ');
  return <Link className={classes} {...rest} />;
}
