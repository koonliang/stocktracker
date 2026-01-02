import type { PasswordValidation } from '@/types/auth'
import styles from './PasswordStrengthIndicator.module.css'

interface Props {
  validation: PasswordValidation
  show: boolean
}

const requirements = [
  { key: 'minLength', label: 'At least 8 characters' },
  { key: 'hasUppercase', label: 'One uppercase letter' },
  { key: 'hasLowercase', label: 'One lowercase letter' },
  { key: 'hasNumber', label: 'One number' },
  { key: 'hasSymbol', label: 'One symbol (!@#$%^&*...)' },
] as const

export function PasswordStrengthIndicator({ validation, show }: Props) {
  if (!show) return null

  return (
    <div className={styles.container}>
      <p className={styles.title}>Password requirements:</p>
      <ul className={styles.list}>
        {requirements.map(({ key, label }) => (
          <li
            key={key}
            className={`${styles.item} ${validation[key] ? styles.valid : styles.invalid}`}
          >
            <span className={styles.icon}>{validation[key] ? '✓' : '○'}</span>
            {label}
          </li>
        ))}
      </ul>
    </div>
  )
}
