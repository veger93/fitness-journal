import type { HTMLAttributes } from 'react';
import styles from './Card.module.css';

interface CardProps extends HTMLAttributes<HTMLElement> {
  variant?: 'default' | 'inverse' | 'accent';
  /** большой блок (график, форма) — скругление и отступы крупнее */
  size?: 'card' | 'section';
  as?: 'div' | 'section' | 'article';
}

export function Card({ variant = 'default', size = 'card', as: Tag = 'div', className, ...rest }: CardProps) {
  const classes = [styles.card, styles[variant], size === 'section' && styles.section, className]
    .filter(Boolean)
    .join(' ');
  return <Tag className={classes} {...rest} />;
}
