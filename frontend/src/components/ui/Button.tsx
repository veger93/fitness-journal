import type { ButtonHTMLAttributes } from 'react';
import styles from './Button.module.css';

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary';
  fullWidth?: boolean;
}

/** Единственная кнопка приложения: две разновидности, форма-"пилюля", высота 52px. */
export function Button({ variant = 'primary', fullWidth = false, className, type = 'button', ...rest }: ButtonProps) {
  const classes = [styles.button, styles[variant], fullWidth && styles.fullWidth, className].filter(Boolean).join(' ');
  return <button type={type} className={classes} {...rest} />;
}
