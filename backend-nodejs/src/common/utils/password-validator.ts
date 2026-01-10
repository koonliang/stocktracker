/**
 * Password validation utility matching Java's PasswordValidator
 * Requirements:
 * - Min 8 characters
 * - Max 72 characters (BCrypt limit)
 * - At least one uppercase letter
 * - At least one lowercase letter
 * - At least one digit
 * - At least one special character
 */
export class PasswordValidator {
  private static readonly MIN_LENGTH = 8;
  private static readonly MAX_LENGTH = 72; // BCrypt limit
  private static readonly UPPERCASE = /[A-Z]/;
  private static readonly LOWERCASE = /[a-z]/;
  private static readonly DIGIT = /\d/;
  private static readonly SYMBOL = /[!@#$%^&*()_+\-=[\]{}|;':",./<>?]/;

  /**
   * Validate password and return list of error messages
   * Returns empty array if password is valid
   */
  static validate(password: string | null | undefined): string[] {
    const errors: string[] = [];

    if (!password || password.length < this.MIN_LENGTH) {
      errors.push(`Password must be at least ${this.MIN_LENGTH} characters`);
    }

    if (password && password.length > this.MAX_LENGTH) {
      errors.push(`Password must not exceed ${this.MAX_LENGTH} characters`);
    }

    if (password) {
      if (!this.UPPERCASE.test(password)) {
        errors.push('Password must contain at least one uppercase letter');
      }
      if (!this.LOWERCASE.test(password)) {
        errors.push('Password must contain at least one lowercase letter');
      }
      if (!this.DIGIT.test(password)) {
        errors.push('Password must contain at least one number');
      }
      if (!this.SYMBOL.test(password)) {
        errors.push(
          'Password must contain at least one symbol (!@#$%^&*()_+-=[]{}|;\':",./<>?)',
        );
      }
    }

    return errors;
  }

  /**
   * Check if password is valid (no errors)
   */
  static isValid(password: string | null | undefined): boolean {
    return this.validate(password).length === 0;
  }
}
