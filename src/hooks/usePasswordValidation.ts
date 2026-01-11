import { useMemo } from 'react'
import type { PasswordValidation } from '@/types/auth'

const MIN_LENGTH = 8
const UPPERCASE_REGEX = /[A-Z]/
const LOWERCASE_REGEX = /[a-z]/
const NUMBER_REGEX = /\d/
const SYMBOL_REGEX = /[!@#$%^&*()_+\-=[\]{}|;':",./<>?]/

export function usePasswordValidation(password: string): PasswordValidation {
  return useMemo(() => {
    const minLength = password.length >= MIN_LENGTH
    const hasUppercase = UPPERCASE_REGEX.test(password)
    const hasLowercase = LOWERCASE_REGEX.test(password)
    const hasNumber = NUMBER_REGEX.test(password)
    const hasSymbol = SYMBOL_REGEX.test(password)
    const isValid = minLength && hasUppercase && hasLowercase && hasNumber && hasSymbol

    return {
      minLength,
      hasUppercase,
      hasLowercase,
      hasNumber,
      hasSymbol,
      isValid,
    }
  }, [password])
}
